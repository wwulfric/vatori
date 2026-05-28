# Agent 指令

适用范围：本文件适用于整个仓库。

## 项目目标

本仓库正在构建 `Vaadin Desktop Framework v0`：一个用于创建桌面级 Vaadin Flow 应用的真实框架骨架，形态类似 Tauri + Vaadin Flow。

目标不是做一个简单的浏览器壳。长期方向是 Flow over IPC：

- 保留 Vaadin Flow 的 Java UI 编程模型。
- 将 Servlet/HTTP/WebSocket 假设隔离在 adapter 后面。
- 桌面 host 拥有系统 WebView、原生能力、生命周期、打包和更新路径。
- IPC 最终成为 bootstrap、event、patch、command 和 capability result 的传输层。

## 当前状态

当前实现是一个真实框架骨架，加上一条 compatibility transport：

- `vaadin-desktop-core`：公开的 Java desktop API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace 基础结构。
- `vaadin-desktop-host`：macOS WKWebView fallback、compatibility host 和 Java native bridge 实现。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 主线的 Swift/AppKit/WKWebView host spike。
- `vaadin-desktop-flow`：真实 Vaadin Flow/Spring Boot launcher，加上 Flow IPC runtime 和 UIDL adapter 切片。
- `vaadin-desktop-devtools`：带 Flow IPC v1 检查的 trace inspector scaffold。
- `vaadin-desktop-cli`：CLI scaffold。
- `examples/*`：真实 Vaadin Flow 示例应用。
- `test-apps/vaadin-desktop-smoke-app`：独立的开发者形态 smoke app。
- `test-apps/flow-over-ipc-smoke-app`：面向受限 `dev-ipc` runtime 切片的独立 no-HTTP Flow IPC smoke app。
- `test-apps/flow-over-ipc-framework-app`：更接近普通开发者项目形态的 no-HTTP Flow IPC validation app，覆盖 Button、BigDecimalField、Checkbox、Select、重复列表更新和 desktop capability；strict trace gate 仍然以 smoke app 为准。
- `prototypes/phase0-phase1-spike`：更早的非 HTTP mock Flow transport / IPC 验证。
- `flow-transport-adapter-spike`：当前代码级 Vaadin Flow UIDL/event/patch 边界分析。
- `vaadin-desktop-flow/src/main/java/dev/vatori/desktop/flow/transport`：围绕 `UidlRequestHandler`、`UidlWriter`、`DesktopVaadinRequest`、`DesktopVaadinResponse` 和 `DesktopFlowRuntimeSession` 的已编译 UIDL adapter 切片。

macOS 上默认 `dev` 行为：

1. 在 `127.0.0.1` 随机端口启动真实 Vaadin Flow/Spring Boot 应用。
2. 启动 `SystemWebViewDesktopHost`。
3. 使用 `/usr/bin/swiftc` 编译临时 Swift/WKWebView `.app` bundle。
4. 打开加载本地 Vaadin URL 的原生桌面窗口。

重要边界：这是真实 desktop WebView host，但 UI transport 仍然是 local HTTP/Servlet/WebSocket compatibility mode。不要把它描述为最终 Flow over IPC。

长期 host 路径：

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

这会运行 `vaadin-desktop-host-swift`：它把 Java app 作为 sidecar 启动，在 sidecar 打印 `Application URL: ...` 后打开原生 AppKit/WKWebView 窗口，以 `HOST_PROTOCOL ...` 形式记录 Host Protocol v0 trace，读取以 `VDP1 ` 为前缀的 Java command envelope，并在窗口关闭时杀掉 Java 进程。它仍然通过 localhost HTTP compatibility mode 加载 Flow UI。

`dev-swift` 通过 macOS LaunchServices 启动生成的 `.build/vaadin-desktop-host-swift/VaadinDesktopHost.app`。日志会从 `.build/vaadin-desktop-host-swift/host.out.log` 和 `.build/vaadin-desktop-host-swift/host.err.log` tail 出来。真实窗口验证时，检查 `HOST_PROTOCOL webview.dom` 和 `.build/vaadin-desktop-host-swift/window-snapshot.png`；在受管 macOS 桌面上，全局截图可能会被 `loginwindow` / MDM overlay 遮挡。

No-HTTP IPC 切片：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

这会用 `--transport flow-ipc` 运行 Swift host，加载本地 WKWebView HTML shell，并通过 `VFIPC1` stdin/stdout envelope 与 Java 通信。它证明了 smoke app 在没有 localhost HTTP 的情况下完成 WebView event -> Java Button listener -> desktop command/capabilityResult -> patch -> WebView。WebView bridge 会把 click/change event、`BigDecimalField` Web Component change event、`Checkbox` Web Component change event、真实 `TextField` Web Component change event 和 ordering-recovery resync 作为 Flow-client-shaped UIDL RPC payload 发送；Java 在 `payload.uidl` 下发出真实 UIDL，其中 bootstrap 和 ordered server-initiated push 使用 `UidlWriter`，event/resync 使用 `UidlRequestHandler` / `ServerRpcHandler`。Bootstrap、ordered push、event patch 和 recovery resync 都由可见 Flow client engine 消费。一个受控 duplicate/late push probe 会被忽略，并记录 delivered/ignored order diagnostics，然后触发显式 resync。Bootstrap 还会声明真实 Vaadin `flow-client` classpath resources 和 v1.4 frontend artifact manifest；WebView 通过 IPC 请求、验证并分类 advertised resource graph，在 `resourceRequest` 中回显 artifact family、module specifier、dependencyOf、executionPolicy、resolver 和 status metadata，同时导入并初始化 `FlowBootstrap.js` 和增强版 `FlowClient.js`。component frontend manifest 会声明 `BigDecimalField`、`TextField`、`Checkbox` 和 `Select` 四个真实 Flow component：BigDecimalField 请求并执行真实 `META-INF/resources/frontend/vaadin-big-decimal-field.js` classpath 组件前端模块，其 `lit` import 解析到 focused extracted Lit CSS helper，`@vaadin/component-base/src/define.js` 解析到 focused extracted define helper，`@vaadin/text-field` import 已改为真实 `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` entry；Checkbox/Select 也通过 IPC 请求聚焦 dev-bundle 入口，从 Vaadin production `indexhtml` chunk 导入真实 class 并注册 `vaadin-checkbox` / `vaadin-select`。v1.5 在 WebView/host 边界加入 no-localhost frontend staging resolver：真实 artifact body 被 stage 为 Blob module URL，每个 staged artifact 都会记录 `resourceStage`，BigDecimalField 和 Checkbox/Select 通过 staged path 执行；TextField production class entry 会通过 staging 尝试，真实 Vaadin dev-bundle `generated-flow-imports-*.js` chunk 会进入 generated/lazy staged execution attempt，并记录当前 `Flow.loadOnDemand` API 边界和浏览器错误。v1.6 把 TextField/npm graph blocker 收敛到更精确的 staged graph 证据。v1.7 通过 stable resource-path Blob module identity 复用 production `indexhtml` staged dependency，越过 v1.6 duplicate custom element definition 层，记录 `textfieldExecutionVersion=flow-ipc-textfield-v1_7`、`textfieldSideEffectVersion=flow-ipc-textfield-side-effect-v1_7`、module identity、side-effect strategy 和 `remainingBlockerClassification=none`，并验证真实 Flow `TextField` Java listener -> UIDL patch -> 可见 DOM `Vaadin TextField: Flow IPC typed`。v1.8 将 generated/lazy staged graph 的 `./markdown-helpers-31I0A-hg.js` relative import 重写到 stable staged dependency，真实 `generated-flow-imports-VW7hRaNw.js` 和 markdown helper 都进入 `resourceStage` executed path，并记录 `flow-ipc-generated-lazy-resolver-v1_8`、resolved dependency、module identity、`frontendArtifactGeneratedLazyExecuted=true` 和 `api-installed-by-staged-generated-flow-imports` 的受控 `Flow.loadOnDemand` probe。v1.9 没有把该 probe 包装成成功，而是把 blocker 收敛为 `flow-ipc-loadondemand-v1_9`：trace 明确记录 API owner、install source、requested chunk、registry/mapping source、resolved artifact path、diagnostic bridge、`frontendArtifactGeneratedLazyFlowClientTriggered=false` 和 `failureLayer=loadondemand-api-installed-by-generated-imports-not-flow-client-triggered`。v1.10 增加真实 Flow `LoadMode.LAZY` dependency runtime path：smoke/framework app 通过普通 `@JavaScript(..., loadMode = LoadMode.LAZY)` 声明 `context://vatori-flow-ipc-lazy-v1_10.js`，可见 Flow DependencyLoader 触发 lazy script，host 将该请求转为 IPC resource resolver，stage 为 Blob module URL 并执行；trace 记录 `flow-ipc-lazy-dependency-v1_10`、`flowLazyDependencyTriggeredByFlowClient=true` 和 `flowLazyDependencyExecuted=true`。v1.10 也把 production bundle 从 `stats.json` verified-only 推进为 runtime registry / staged execution attempt：stats 选择 `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`，并以 `flow-ipc-production-runtime-v1_10` blocker 记录 selected artifact、dependency chain、module identity 和 production runtime ownership failure layer。v1.11 捕获真实 bootstrap UIDL `Flow.loadOnDemand('<chunkId>')` expression，观察可见 Flow client / DependencyLoader 触发，映射到真实 `generated-flow-imports-VW7hRaNw.js` artifact，以 stable resource-path module identity stage/import，并把剩余 blocker 收敛为 `flow-ipc-loadondemand-chunk-v1_11` / `failureLayer=dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file`。v1.12 解析真实 generated-flow-imports body，证明当前 Vaadin build 的 `loadOnDemand` table 是 `Promise.resolve(0)` no-op，没有生成 `./chunks/chunk-*.js` 分支，并记录 `flow-ipc-loadondemand-chunk-file-v1_12`、`chunkFileStatus=not-generated`、callback resolved 和 `chunk-file-not-generated` failure layer。v1.13 将 production runtime staged attempt 拆成明确 owner contract：Java manifest / response 声明 `productionBundleRuntimeOwnerVersion=flow-ipc-production-owner-v1_13`、`productionBundleRuntimeOwner=flow-ipc-production-bundle-runtime`、entry artifact、stats source、owner module identity、cache/cleanup 和 component reuse 语义；Swift 用独立 owner namespace stage 真实 production `indexhtml` artifact，并在 `resourceStage` / `webview.dom` 记录 `owner=production-bundle-runtime`、registry before/after、side-effect proof、`componentProductionOwnerReused=false` 和 `production-owner-runtime-contract-missing` blocker。v1.14 让该 owner 对同一个真实 stats-selected `indexhtml` artifact 发起受控 dynamic import / side-effect probe，记录 `productionBundleRuntimeOwnerImportVersion=flow-ipc-production-owner-import-v1_14`、module identity、relative dependency rewrite result、resolved/unresolved specifiers、`window.Vaadin` before/after、custom element registry before/after、browser error/name、component owner reuse=false，并把当前最终 blocker 收敛到 `production-owner-relative-import-unresolved`，同时保留浏览器 duplicate custom element evidence。剩余 TextField shim 只作为旧路径回归保护，不能计入 v1.7+ 成功；真实 app/build generated chunk-file parity、production owner import/side-effect success 和 dev-mode/Vite execution 仍是 blocker。这不是完整 production bundle/dev-mode/theme parity。它还会对 `Flow.js` 执行动态 no-HTTP `flow-client-lazy-lookup` probe，并记录 static/theme/lazy lookup diagnostics。仓库内的本地 UIDL applier 只保留为 non-engine 或 failed-delivery 路径的诊断 fallback。

v1.15 已把 v1.14 的 production owner import blocker 继续收敛到 owner-scoped dependency resolver closure：Java manifest / response 声明 `productionBundleRuntimeOwnerDependencyResolverVersion=flow-ipc-production-owner-deps-v1_15`、resolver root/policy、expected artifacts、acceptable failures 和 phase diagnostics；Swift host 递归扫描真实 `indexhtml` relative imports，请求并 stage 真实存在的 dev-bundle build artifacts，记录 graph counts、resolved/missing classifications、phase candidates、missing paths/specifiers，并把当前 accepted blocker 从未分类的 `production-owner-relative-import-unresolved` 收敛为 `production-owner-missing-real-artifact`。v1.16 在此基础上加入 production owner import ordering / component reuse 边界：manifest / response 声明 `productionBundleRuntimeOwnerImportOrderingVersion=flow-ipc-production-owner-ordering-v1_16` 和 `componentProductionOwnerReuseVersion=flow-ipc-component-owner-reuse-v1_16`；Swift host 记录 `before-component-imports` owner-first preflight、after-component fallback、observed phases、component reuse decision，以及当前 `production-owner-duplicate-custom-element` / component reuse contract blocker。v1.17 增加 host-owned frontend resource loader 边界：Java manifest / resource response 声明 `frontendResourceLoaderVersion=flow-ipc-resource-scheme-v1_17`、`custom-scheme,file-backed,blob-fallback` 和 preferred `custom-scheme-file-backed`；Swift host 安装 `vatori-resource` `WKURLSchemeHandler`，把 Flow `LoadMode.LAZY` dependency body 落盘到 `.build/vaadin-desktop-host-swift/flow-ipc-resources` 后用 `vatori-resource://...` 导入执行，并在 `resourceLoad` / `resourceStage` / `webview.dom` 中记录 handler install、request/response、MIME、redacted file path、no-localhost proof、relative import base 和 cleanup policy。v1.18 把该 host-owned loader 扩展到 production owner root/dependency graph：Java manifest / response 声明 `productionBundleRuntimeOwnerResourceLoaderVersion=flow-ipc-production-owner-resource-loader-v1_18`、owner graph mode、`vatori-resource` scheme、stable source-path base policy 和 root Blob fallback disabled；Swift host 将 stats-selected `indexhtml` root 和已解析 owner dependencies 注册为 `vatori-resource://flow-ipc/production-owner/...` file-backed URL，记录 root/dependency handler request/response、module identity、owner-first import over custom scheme、component reuse decision 和 custom-scheme graph 层 blocker。v1.19 在 custom-scheme owner import 之后拆分 module import result 与可复用 side effect result：Java manifest / response 声明 `productionBundleRuntimeOwnerSideEffectVersion=flow-ipc-production-owner-side-effect-v1_19` 和 `componentProductionOwnerReuseDecisionVersion=flow-ipc-component-owner-reuse-decision-v1_19`；Swift host 记录 expected tags/globals、before/after registry/global diagnostics、`productionBundleRuntimeOwnerModuleImported`、`productionBundleRuntimeOwnerSideEffectSatisfied` 和当前 `focused-fallback-after-owner-side-effect-check` decision。v1.20 把 production owner-first custom-scheme runtime attempt 移到 focused component imports 之前：manifest / response 声明 `productionBundleRuntimeOwnerOwnedRuntimeVersion=flow-ipc-production-owner-owned-runtime-v1_20` 和 `componentProductionOwnerReusePolicyVersionV1_20=flow-ipc-component-owner-reuse-policy-v1_20`；Swift host 记录 owner-first phase、expected tags/globals、no-Blob-fallback contract、owner-owned runtime satisfied/failure layer 和 component resolver decision。当前 accepted trace 是 `production-owner-owned-missing-real-artifact` + `focused-fallback-after-owner-owned-runtime-attempt`，不是 production owner side-effect success、完整 production bundle/dev-mode/theme parity 或真实 app/build generated chunk-file parity。v1.21 将当前 resource evidence 收束为统一 frontend resolver contract：manifest / response / stage / load / DOM summary 记录 `frontendResolverContractVersion=flow-ipc-frontend-resolver-contract-v1_21`、covered families、sha256 integrity、stable identity、window-lifetime cache/cleanup、allowed schemes `blob,vatori-resource`、verified-only/strict-blocker/accepted-gap 分类和 no-localhost proof；这仍不是 production owner success、真实 app/build generated chunk-file parity 或 dev-mode/Vite parity。v1.22 增加 Vaadin 同形状 generated chunk-file 正向执行 fixture：Java 声明 `generatedLazyChunkFileExecutionVersion=flow-ipc-loadondemand-chunk-file-execution-v1_22`、fixture imports/chunk artifacts、`loadOnDemandTableShape=branch-table-dynamic-import`、`chunkFileStatus=available` 和 side-effect proof；Swift host 通过同一 IPC resolver 请求 fixture generated-flow-imports 和 chunk body，stage/import 后调用 fixture `Flow.loadOnDemand(chunkId)`，并记录 callback 与 `window.VaadinDesktopGeneratedLazyChunkV122.executed` side effect 成功。这不是当前 app/build 真实生成 lazy chunk；v1.12 no-op table blocker 仍保留。v1.23 增加 Vaadin 同形状 production owner positive runtime fixture，证明 owner root/dependency contract 存在时 custom-scheme owner path 可以执行并产生可复用 side effect；v1.24 将 v1.21 resolver、v1.22/v1.23 positive fixtures、真实 app/build blockers 和 `packaging-time-staged-frontend-or-supported-vaadin-build-spi` product path decision 收束成 `flow-ipc-frontend-pipeline-closure-v1_24` trace contract。v1.25 将该 product path 落成 `flow-ipc-packaged-frontend-baseline-v1_25`：manifest / response / stage / load / DOM summary 记录 host-owned inventory、preferred `custom-scheme-file-backed` loader、diagnostic Blob-only fallback、sha256/byte-count integrity、window-lifetime cleanup 和 real-build blocker preservation；它不是完整 installer packaging、真实 generated chunk parity、production owner parity 或 dev-mode/Vite parity。v1.26 将 generated lazy blocker 收敛到 Vaadin source/build boundary：manifest / response / stage / DOM summary 记录 `flow-ipc-generated-lazy-source-boundary-v1_26`、`AbstractUpdateImports` / `FrontendDependencies`、lazy route / `DependencyTrigger` 输入、当前真实 generated-flow-imports no-op table，以及必须通过 app build 或受支持 Vaadin lazy chunk SPI 产生真实 chunk file 的下一步决策。v1.27 将该 source/build boundary 推进为真实 app/build probe：smoke app 新增普通 Flow `@Route("flow-ipc-lazy-probe-v1_27")` + `@JavaScript(loadMode = LoadMode.LAZY)` 输入，manifest / response / stage / DOM summary 记录 `flow-ipc-generated-lazy-real-build-probe-v1_27`、route/class/dependency observed、scanner observed、lazy JavaScript observed、当前 generated-flow-imports no-op table、`chunkStatus=not-generated` 和 `scanner-output-not-connected-to-dev-bundle-generated-imports` failure layer。v1.28 继续把这个黑箱拆成 build lifecycle evidence：trace 记录 `flow-ipc-generated-lazy-build-lifecycle-v1_28`、scanner source/status、build/update-imports 状态、runtime artifact source、redacted jar path、mtime、byte count、sha256、write target、`fixtureSubstituted=false`、no-localhost proof，并把当前 failure layer 收敛为 `runtime-using-stale-packaged-dev-bundle`。v1.29 连接当前 app build output discovery：trace 记录 `flow-ipc-generated-lazy-app-build-output-v1_29`、app module/root、generated imports observed=false、table shape `missing`、chunk status `missing-build-output`、runtime source `packaged-dev-bundle-fallback`、fallback reason `current-app-build-output-generated-imports-missing`、SPI assessment `requires-build-step` 和 failure layer `app-build-output-missing-generated-imports`。v1.30 增加 opt-in 当前 app frontend build output materialization：`VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1` 会运行可审计 Vaadin frontend build preflight，trace 记录 `flow-ipc-current-app-frontend-build-output-v1_30`、build command/status/log、output inventory、generated imports candidates、runtime source 和当前离线 blocker `vaadin-frontend-build-step-unavailable`。v1.31 增加 frontend build preflight availability：显式 online/local-repo preflight 可解析 `com.vaadin:vaadin-maven-plugin:25.1.5` 并成功运行 `prepare-frontend`，trace 记录 `flow-ipc-frontend-build-preflight-availability-v1_31`、plugin resolution、goal result、output usability 和当前 `frontend-build-plugin-resolved-output-incomplete` blocker。v1.32 增加 current app build-frontend output contract：显式 full `build-frontend` 产生当前 app source generated imports/chunk、production output 和 `prod.bundle`，并把 blocker 收敛为 `current-app-source-generated-chunk-npm-specifier-unresolved`。v1.33 进一步增加 current app generated chunk package-edge contract：当前 app generated chunk 内的 `@vaadin/vertical-layout/src/vaadin-vertical-layout.js` 被重写到稳定 production `indexhtml` module identity，`Flow.loadOnDemand(chunkId)` 路径 stage/import 当前 app generated imports/chunk，并记录 `loadOnDemandChunkFileExecuted=true`。v1.34 移除 current app generated imports root diagnostic placeholder：root bare imports 解析到当前 app production `generated-flow-imports-*.js` support artifact 和 production webapp build graph，trace 记录 placeholder=false、root unresolved 为空、support artifact staged/executed 和 root package graph executed。v1.35 将 v1.32-v1.34 的 current app source/prod artifact 证据收束成 productized runtime ownership handoff：trace 记录 `flow-ipc-current-app-production-runtime-ownership-v1_35`、primary candidate、selected source imports/chunk、production support artifact、production entry、stats、`prod.bundle`、artifact inventory、no-localhost/no-fixture proof、`handoffReady=true`，并明确 owner side-effect/reuse=false 和 `current-app-production-runtime-ownership-handoff-pending`。v1.36 将该 handoff candidate 接入 current app production owner import：root/dependency graph 通过 host-owned `vatori-resource://` custom scheme stage/import，trace 记录 `currentAppProductionOwnerImported=true`、`currentAppProductionOwnerImportSideEffectSatisfied=true`、side-effect proof 和 `currentAppProductionOwnerImportReuseSatisfied=false`。v1.37 将该 side-effect proof 接入 current app production owner component reuse：当 owner 已注册 `vaadin-text-field`，host 跳过 focused TextField dev-bundle import，trace 记录 `currentAppProductionOwnerComponentReuseDecision=current-app-owner-reused`、`currentAppProductionOwnerComponentReused=true` 和可见 Flow `TextField` event/patch/DOM 成功；剩余 blocker 是更早组件执行顺序的泛化 owner reuse、真实 app/build generated chunk-file parity、dev-mode/Vite 和更广组件/资源覆盖。

注意：v1.12 的 generated chunk-file gate 现在以 no-op table / missing chunk-file proof 为准；在后续更复杂 trace 中，callback resolution 字段可能因下游 strict blocker 为 false，不能把它单独当成 chunk-file success 条件。

可重复的真实窗口 smoke 验证可以使用：

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

## 构建和运行

本机 Maven 通过 SDKMAN 可用。直接调用 Maven 前先 source SDKMAN：

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
```

使用项目本地 Maven repository，避免写入用户全局 Maven cache：

```bash
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

运行 smoke desktop app：

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

运行 Swift native host spike：

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

运行 Flow IPC smoke app：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

运行 Flow IPC framework validation app：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

可重复的 framework app 真实窗口验证可以使用：

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

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
```

验证 server-side UIDL writer adapter：

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

Host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

启动 system WebView host 会打开 macOS UI 进程，可能需要 sandbox escalation。结束一个 turn 前，应停止长时间运行的 dev session。

## 本地参考源码树

用户本机有上游源码 checkout 可供研究。除非用户明确要求编辑，否则把它们视为只读参考。

Tauri，仅作为历史参考：

```text
/Users/bytedance/RustroverProjects/study/tauri
```

如果要对比 Tauri 的进程模型，常用区域：

- `crates/tauri`
- `crates/tauri-runtime`
- `crates/tauri-runtime-wry`
- `crates/tauri-cli`
- `crates/tauri-bundler`
- `crates/tauri-utils`

Vaadin：

```text
/Users/bytedance/IdeaProjects/study/vaadin
```

常用 Vaadin 区域：

- `flow/flow-server`
- `flow/flow-client`
- `flow/flow-push`
- `flow/vaadin-spring`
- `platform/vaadin-spring-boot-starter`
- `platform/vaadin-maven-plugin`
- `platform/vaadin-dev-bundle`

做架构决策时，优先检查这些本地源码树，再考虑 web search。

## 文档地图

- `README.md`：当前 quick start 和状态。
- `docs/framework-architecture.md`：当前架构与目标架构。
- `docs/getting-started.md`：开发者 workflow。
- `docs/flow-integration.md`：今天如何集成真实 Vaadin Flow。
- `docs/ipc-protocol.md`：protocol envelope 和 message taxonomy。
- `docs/ipc-protocol-v1.md`：Flow IPC v1 envelope 和 message taxonomy。
- `docs/flow-over-ipc-runtime.md`：当前 `dev-ipc` runtime 切片和限制。
- `docs/flow-transport-adapter.md`：当前 adapter 实现和下一个 UIDL 边界。
- `docs/acceptance-tools.md`：trace gate、smoke harness、真实窗口验收 hook 和 runtime diagnostics 的边界说明。
- `docs/native-host.md`：当前 WKWebView host 和 host roadmap。
- `docs/roadmap.md`：下一阶段工程路线。
- `docs/flow-over-ipc-design.md`：原始中文设计包。
- `docs/flow-over-ipc-implementation.md`：Phase 0/1 prototype notes。

## 项目技能

- `.agents/skills/vaadin-desktop-native-feel`：用于让 Vaadin Desktop app 具备原生桌面体验的项目级 skill。当工作触及 Swift/AppKit host 行为、WKWebView/WebView2 quirk、Vaadin Flow desktop UI 约定、desktop capability API、compatibility-mode 边界或 native-readiness review 时使用它。

## 工程规则

- 保持 compatibility mode 被明确命名和记录。
- 不要移除或隐藏当前 Flow 仍然依赖 Servlet/HTTP/WebSocket 的事实。
- 不要把框架收缩成一次性的示例应用。
- 保持 example app 是开发者形态：它们应该像外部用户一样依赖 framework modules。
- 除非明确要求，不要编辑本地 Tauri 或 Vaadin upstream source checkout。
- 优先使用小而可 review 的模块和清晰 adapter 边界。
- 搜索源码时使用 `rg` / `rg --files`。
- 文件编辑使用 `apply_patch`。
- 每轮工作按适合粒度提交 commit；提交信息使用 Conventional Commit 前缀，例如
  `feat:`、`fix:`、`docs:`、`test:`、`chore:`、`refactor:`。
- 除非明确要求，不要删除生成文件或用户文件。

## 近期工作

最有价值的下一步：

1. 在 `vaadin-desktop-host-swift` 跨本地机器稳定后，把它作为默认 host。
2. 将 Host Protocol v0 trace 推进为 bidirectional command/result transport。
3. 基于 v1.37 current app production owner TextField reuse，继续推进更早组件执行顺序的泛化 owner reuse，或接入受支持 Vaadin lazy chunk SPI、dev-mode/Vite execution。
4. 为 Swift host window launch、sidecar cleanup 和 desktop capability call 增加 smoke tests。
5. 将受控 duplicate/late push probe 推进为 durable queue、ack、reconnect 和 resync 语义。
