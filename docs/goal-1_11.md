# Goal 1.11: Generated Flow.loadOnDemand Chunk Runtime Path

`docs/goal.md` 是长期愿景和完成度账本。v1.10 已经证明普通
`LoadMode.LAZY` dependency 可以由可见 Flow client / `DependencyLoader` 触发，并经
no-localhost IPC resolver stage + execute；production bundle 也已经从 verified-only 推进到
runtime registry / staged execution attempt。

本阶段要继续推进长期账本中最核心的 frontend blocker：**generated/lazy
`Flow.loadOnDemand(chunkId)` 必须从 v1.9 的 diagnostic bridge 走向真实 Flow
client engine 触发的 chunk runtime path。**

**核心目标：让 no-localhost `dev-ipc` runtime 能识别、映射、stage 并执行真实 UIDL
lazy expression 中的 generated `Flow.loadOnDemand('<chunkId>')` chunk 请求；如果仍无法
执行成功，必须把 blocker 收敛到 chunk registry、Flow client API ownership、module
identity 或 side-effect integration 的精确层级。**

## 继承边界

以下能力在 v1.11 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC runtime，核心 UI path 不通过 localhost HTTP。
- Visible Flow client engine 继续负责 bootstrap、event patch、push、resync 和普通 lazy
  dependency 的可见 renderer。
- v1.7 TextField graph execution 继续通过：真实 `vaadin-text-field` graph 经 stable
  resource-path module identity 执行，Java listener -> UIDL patch -> visible DOM 链路继续可验证。
- v1.8 generated/lazy artifact resolver 继续通过：`generated-flow-imports-*.js` 及其
  relative dependency 能通过 stable staged module identity 执行。
- v1.9 loadOnDemand contract evidence 继续可追踪：API owner、install source、requested
  chunk、registry/mapping source、resolved artifact path、diagnostic bridge 和
  `frontendArtifactGeneratedLazyFlowClientTriggered=false` 的历史 blocker 语义不能丢失。
- v1.10 普通 `LoadMode.LAZY` dependency runtime path 继续通过：
  `flowLazyDependencyTriggeredByFlowClient=true`、`flowLazyDependencyExecuted=true`。
- v1.10 production runtime registry / staged execution attempt 继续通过，并保留
  `production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports` blocker。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync 和
  WebView error-reporting baseline 继续可验证。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。
- 旧 TextField shim、direct staged probe、diagnostic bridge 和普通 `LoadMode.LAZY`
  dependency 都不能单独计入 v1.11 成功。

## 本地 Vaadin 合同依据

制定和实现 v1.11 时要优先参考本机 Vaadin 源码：

- `flow-build-tools/src/main/java/com/vaadin/flow/server/frontend/AbstractUpdateImports.java`
  生成 `generated-flow-imports.js`，其中 `loadOnDemand(key)` 按 chunk key 动态
  `import('./chunks/<chunk>.js')`，并安装到 `window.Vaadin.Flow.loadOnDemand`。
- `flow-server/src/test/java/com/vaadin/flow/server/communication/UidlWriterTest.java`
  证明 UIDL dependency 中会出现
  `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` 形式的 lazy expression。
- `flow-client/src/main/java/com/vaadin/client/DependencyLoader.java` 负责按 UIDL dependency
  metadata 调度 eager、inline、deferred 和 lazy dependency。
- `flow-server/src/main/java/com/vaadin/flow/server/communication/UidlWriter.java` 和
  `flow-server/src/main/java/com/vaadin/flow/server/communication/IndexHtmlRequestHandler.java`
  是确认 dependency serialization、chunk key 和 generated imports bootstrap 关系的关键边界。

v1.11 的实现应围绕这些真实合同做 no-localhost adapter，不新增 repository-local fake
`loadOnDemand` API 来冒充 Flow client engine 行为。

## 当前 blocker

截至 v1.10，已经证明：

- 真实 generated-flow-imports dev-bundle artifact 和 relative dependency 可以 stage + execute。
- v1.9 trace 能说明当前 `Flow.loadOnDemand` API 是由 generated imports direct staged import
  安装，再由 diagnostic bridge 调用。
- 普通 `@JavaScript(..., loadMode = LoadMode.LAZY)` 可以通过 visible Flow client /
  `DependencyLoader` 触发 IPC resource request，并执行成功。
- Production bundle artifact registry 已能选中真实 artifact 并进入 staged execution attempt。

仍未完成的是：

- `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` 这类真实 UIDL lazy expression 还没有
  由 visible Flow client engine 触发成功。
- 真实 chunk id 到 generated-flow-imports chunk artifact 的 registry/mapping 仍没有成为
  runtime contract。
- v1.9 的 `generatedLazyRequestedChunk=vatori-flow-ipc-generated-lazy-v1_9-probe` 仍是受控
  probe key，不是来自真实 Flow dependency chunk id。
- 如果 chunk 执行后只产生 side effect，还缺少 side-effect ownership、visible state 绑定和
  success 判定。

## 实现范围

### 1. 真实 UIDL Lazy Expression 捕获

目标：

- 在 smoke app 或 focused validation path 中制造一个真实 Flow generated/lazy dependency，
  让 `UidlWriter` 产出
  `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` expression。
- Runtime trace 必须记录该 expression 来自真实 UIDL dependency，而不是 diagnostic bridge：
  - `generatedLazyExpressionSource`
  - `generatedLazyExpression`
  - `generatedLazyChunkId`
  - `generatedLazyTriggerOwner`
  - `generatedLazyTriggeredByFlowClient`
  - `generatedLazyDiagnosticBridgeUsed`
  - `generatedLazyDirectImportProbeUsed`
- 如果真实组件 dependency 很难稳定触发，可以新增一个 focused route/component 作为触发器；
  但触发器必须仍通过 Vaadin Flow server-side API 进入 UIDL，不能写 host-specific JS。

验收要求：

- v1.11 fixture trace 中必须能看到真实 UIDL lazy expression。
- `generatedLazyTriggeredByFlowClient=true` 才能计入成功；否则必须明确记录为什么 Flow client
  仍没有触发。

### 2. Chunk Registry / Mapping Contract

目标：

- 建立真实 chunk id -> generated-flow-imports runtime mapping：
  - chunk id
  - generated imports artifact
  - chunk artifact path
  - source manifest / dev-bundle source
  - dependency chain
  - module identity key
  - execution policy
  - cache policy
  - cleanup owner。
- Mapping 必须能解释 chunk id 来源：
  - UIDL expression literal
  - generated-flow-imports registry
  - build-time imports metadata
  - dev-bundle stats/config source。
- WebView/host/Java trace 必须能区分：
  - v1.9 diagnostic probe key
  - v1.10 ordinary `LoadMode.LAZY` resource path
  - v1.11 generated chunk id。

验收要求：

- 至少一个真实 chunk id 被 resolver 映射到真实 generated/lazy artifact。
- 如果 mapping 失败，blocker 必须是 v1.11 新 blocker，例如
  `flow-ipc-loadondemand-chunk-v1_11`，并记录 failure layer：
  `uidl-expression-detected-no-chunk-registry`、
  `chunk-id-to-artifact-mapping-missing`、
  `generated-imports-registry-not-visible-to-flow-client` 或等价精确分类。

### 3. Flow Client Triggered Chunk Execution

目标：

- 当 visible Flow client engine 调用 `window.Vaadin.Flow.loadOnDemand(chunkId)` 时，runtime
  必须将 chunk artifact 解析为 no-localhost staged module URL，并执行或返回明确 blocker。
- `resourceRequest` / `resourceResponse` / `resourceStage` / `webview.dom` 必须记录：
  - `loadOnDemandContractVersion=flow-ipc-loadondemand-chunk-v1_11`
  - `loadOnDemandApiOwner`
  - `loadOnDemandInstallSource`
  - `loadOnDemandRequestedChunk`
  - `loadOnDemandResolvedArtifact`
  - `loadOnDemandStageUrl`
  - `loadOnDemandModuleIdentity`
  - `loadOnDemandTriggeredByFlowClient`
  - `loadOnDemandExecutionResult`
  - browser/native error 和 failure layer。
- 如果 chunk 执行成功但没有 visible DOM 变化，必须补充 side-effect proof：
  registry mutation、custom element registration、module export/import result、Flow client
  callback resolution 或明确的 app-visible marker。

验收要求：

- Preferred：真实 UIDL lazy expression 经 Flow client engine 触发，chunk stage + execute 成功，
  并产生可追踪 side effect。
- Acceptable：仍未成功执行，但 blocker 必须精确到 runtime 层，并证明已经越过 v1.9
  diagnostic bridge 和 v1.10 ordinary lazy dependency 层。

### 4. Inspector Gate Semantics

新增或明确两类 gate：

- `--require-flow-ipc-v1_11-loadondemand-chunk`：只验证 v1.11 generated
  `Flow.loadOnDemand(chunkId)` delta。
- `--require-flow-ipc-v1_11`：累计阶段完成 gate，表示 v1.10 baseline +
  v1.11 generated chunk delta 都必须通过。

规则：

- `--require-flow-ipc-v1_11` 不能只继承 v1.10 后直接返回成功。
- v1.11 delta 不能用普通 `LoadMode.LAZY` dependency、direct staged import 或 diagnostic
  bridge 代替。
- 如果 v1.11 以 blocker 形式完成，该 blocker 必须来自真实 UIDL lazy expression 或真实
  Flow client trigger，不接受 synthetic-only probe。
- v1.6/v1.9 历史 blocker gate 仍可单独验证，但不应重新成为 cumulative path 的成功条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_11.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.11 cumulative gate 和 delta gate。
- 保持 v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9/v1.10 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 5. Runtime Resolver / Test Surface

目标：

- 将 v1.10 frontend runtime resolver contract 扩展到 generated chunk id：
  - logical request id
  - artifact family
  - resolver source
  - chunk registry
  - path mapping
  - dependency graph
  - stable module identity
  - execution policy
  - cache hit/miss
  - cleanup ownership
  - blocker classification。
- `FlowClientResourceSmokeMain` 或新增 test-source smoke 必须验证 v1.11 metadata，不只依赖
  真实窗口手工 trace。
- `test-apps/flow-over-ipc-framework-app` 至少保持 v1.10 lazy/runtime validation，不因 v1.11
  focused smoke path 退化。

### 6. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或新增 focused v1.11 protocol note
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/acceptance-tools.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`
- 如 framework app 行为变化，也更新对应 README。

文档必须明确：

- v1.9 diagnostic `Flow.loadOnDemand` bridge、v1.10 ordinary `LoadMode.LAZY` dependency 和
  v1.11 generated `Flow.loadOnDemand(chunkId)` 的区别。
- chunk id 来自哪里、如何映射到 artifact、由谁触发、是否执行成功。
- 成功证据是 Flow-client-triggered execution、side-effect proof、visible DOM proof 还是
  precise blocker。
- 如果仍有 blocker，它位于 UIDL expression、chunk id registry、generated imports API
  ownership、module identity、browser execution、side-effect integration 或 Flow client
  callback integration 的哪一层。

## 验收命令

最小验收集合：

```bash
git diff --check
bash bin/flow-client-resource-smoke
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
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
```

真实窗口 smoke 验证：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_GENERATED_LAZY=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

framework app 回归：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

完整编译：

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

## 非目标

- 不要求一次完成 production bundle owned runtime execution；该 blocker 仍可留给后续阶段。
- 不要求一次完成 dev-mode/Vite execution。
- 不要求扩大到 Grid/ComboBox/Dialog/Upload 等 broader component parity。
- 不要求把 macOS Swift host 默认化。
- 不允许把 localhost compatibility mode、direct staged import、diagnostic bridge 或普通
  `LoadMode.LAZY` dependency 描述为 v1.11 generated chunk 成功。

## 完成标准

v1.11 完成时应满足：

- `docs/traces/flow-ipc-v1_11.ndjson` 存在并通过 v1.11 delta / cumulative inspector gate。
- Trace 明确区分 v1.9 diagnostic bridge、v1.10 ordinary lazy dependency 和 v1.11 generated
  `Flow.loadOnDemand(chunkId)`。
- 至少一个真实 UIDL lazy expression 被捕获，并进入 chunk mapping + stage/execution 或
  precise blocker。
- 文档同步更新长期账本和 runtime 限制，不把 blocker 包装成最终成功。
- 所有验收命令中适用于当前平台的命令通过；如果某项因环境限制无法运行，必须在提交说明中记录。
- 每轮工作按适合粒度提交 Conventional Commit。
