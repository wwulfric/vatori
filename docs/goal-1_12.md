# Goal 1.12: Generated LoadOnDemand Chunk File Registry and Side-Effect Proof

`docs/goal.md` 是长期愿景和完成度账本。v1.11 已经把 generated
`Flow.loadOnDemand(chunkId)` 从 v1.9 diagnostic bridge 推进到真实 UIDL lazy expression
和可见 Flow client trigger：host 能捕获
`return window.Vaadin.Flow.loadOnDemand('<chunkId>');`，并把 Flow-client-triggered chunk
request 通过 no-localhost IPC 映射到真实 `generated-flow-imports-VW7hRaNw.js` artifact。

v1.11 的剩余 blocker 很清楚：当前 staged generated-flow-imports module 只证明了
chunk id -> generated-flow-imports artifact mapping，还没有证明 chunk id -> real
`./chunks/chunk-*.js` file mapping、chunk file execution、callback resolution 或 side-effect
ownership。

**核心目标：为 generated `Flow.loadOnDemand(chunkId)` 建立真实 chunk-file registry。runtime
必须能从真实 `generated-flow-imports` / dev-bundle stats / build metadata 中推导 chunk id 到
真实 chunk artifact 的映射，并在 no-localhost IPC resolver 中 stage + execute 该 chunk；如果
当前 app/build 确实没有生成 chunk file，必须把 blocker 收敛为可审计的 no-chunk-generated
证据，而不是继续停留在 v1.11 的 generic generated-flow-imports no-op blocker。**

## 继承边界

以下能力在 v1.12 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC runtime，核心 UI path 不通过 localhost HTTP。
- 可见 Flow client engine 继续负责 bootstrap、event patch、ordered push、resync、ordinary
  `LoadMode.LAZY` dependency 和 generated `Flow.loadOnDemand(chunkId)` trigger。
- v1.7 TextField graph execution 继续通过，旧 TextField shim 只能作为 legacy fallback。
- v1.8 generated/lazy staged resolver 继续能执行 `generated-flow-imports-*.js` 和
  `markdown-helpers-*.js` stable staged dependency。
- v1.9 diagnostic loadOnDemand bridge 继续保留为历史 blocker evidence，但不能计入 v1.12 成功。
- v1.10 ordinary `LoadMode.LAZY` dependency path 继续通过，不能替代 v1.12 generated chunk 成功。
- v1.10 production runtime staged attempt 继续通过，并保持 blocker 显式。
- v1.11 真实 UIDL expression、Flow-client-triggered chunk request、generated-flow-imports mapping
  和 `flow-ipc-loadondemand-chunk-v1_11` evidence 继续通过。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、repeated/list
  update、desktop capability、file transfer、push ordering、resync 和 WebView error-reporting
  baseline 继续可验证。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。

## 本地 Vaadin 合同依据

实现 v1.12 时优先参考本机 Vaadin 源码：

- `/Users/bytedance/IdeaProjects/study/vaadin/flow/flow-build-tools/src/main/java/com/vaadin/flow/server/frontend/AbstractUpdateImports.java`
  - 当 `lazyJavascript` / `lazyCss` 非空时，生成
    `const loadOnDemand = (key) => { ... pending.push(import('./chunks/chunk-*.js')); ... }`。
  - 当没有 lazy chunk 时，生成
    `const loadOnDemand = (key) => { return Promise.resolve(0); }`。
  - 最后把函数安装到 `window.Vaadin.Flow.loadOnDemand`。
- `/Users/bytedance/IdeaProjects/study/vaadin/flow/flow-server/src/main/java/com/vaadin/flow/component/internal/UIInternals.java`
  - `addComponentDependencies` 会把 component class chunk id 转成
    `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` dynamic import expression。
- `/Users/bytedance/IdeaProjects/study/vaadin/flow/flow-server/src/test/java/com/vaadin/flow/server/communication/UidlWriterTest.java`
  - 证明 production/development mode 下 UIDL dependency 里都可能出现
    `Flow.loadOnDemand('<chunkId>')` key，并且 production mode 可能为父类也发送 chunk id。
- Vaadin dev-bundle / `stats.json` / generated frontend folder 需要作为 real artifact source
  分析，不允许用 repository-local fake chunk table 冒充 Vaadin 生成结果。

## 当前 blocker

截至 v1.11，已经证明：

- 真实 UIDL lazy expression 可被捕获。
- 可见 Flow client / `DependencyLoader` 会触发 `window.Vaadin.Flow.loadOnDemand(chunkId)`。
- Host/WebView bridge 可以把真实 chunk id 转成 IPC `resourceRequest`。
- Resolver 可以把该请求映射到真实 `generated-flow-imports-VW7hRaNw.js` artifact。
- 该 artifact 可以通过 stable resource-path module identity stage/import。

仍未完成的是：

- 没有从真实 generated-flow-imports body 中抽取 `key -> ./chunks/chunk-*.js` branch。
- 没有把 `./chunks/chunk-*.js` 相对路径解析到真实 classpath/dev-bundle artifact。
- 没有 stage/import real chunk file。
- 没有证明 Promise/callback resolution、module side effect、custom element registry mutation、
  Flow client continuation 或 app-visible marker。
- 如果当前 generated-flow-imports 是 no-op table，trace 还没有证明“为什么没有 chunk file”
  来自真实 Vaadin build output，而不是 resolver 没找对。

## 实现范围

### 1. Chunk Registry Extraction

目标：

- 增加一个 focused `generated-flow-imports` chunk registry parser，输入为真实 resource body，
  输出：
  - `chunkId`
  - `chunkImportSpecifier`，例如 `./chunks/chunk-<hash>.js`
  - `chunkArtifactPath`
  - `chunkContentHash`
  - `registrySource`
  - `mappingSource`
  - `generatedImportsArtifact`
  - `noOpLoadOnDemand`
  - `noChunkGeneratedReason`
  - `sourceBuildMode` / `sourceArtifactFamily`。
- Parser 必须识别两种真实 Vaadin 输出：
  - `if (key === '...') pending.push(import('./chunks/chunk-*.js'));`
  - `const loadOnDemand = (key) => { return Promise.resolve(0); }`。
- 如果当前 fixture 仍然是 no-op table，必须记录 raw evidence：
  `loadOnDemandTableShape=no-op-promise-resolve-0`，并解释是 app/build 没生成 lazy chunk，
  还是 resolver 没有找到 chunk folder。

验收要求：

- `FlowClientResourceSmokeMain` 或新增 focused smoke 能验证 parser metadata。
- Trace 中必须出现 `generatedLoadOnDemandChunkFileRegistryVersion=flow-ipc-loadondemand-chunk-file-v1_12`
  以及 chunk registry source / table shape。

### 2. Real Chunk Artifact Resolver

目标：

- 对非 no-op registry，resolver 必须把 `./chunks/chunk-*.js` 转成真实 artifact path：
  - generated frontend classpath path；
  - Vaadin dev-bundle webapp path；
  - production build path；
  - 或明确的 not-found classification。
- `resourceRequest` / `resourceResponse` 必须携带：
  - `purpose=generated-loadondemand-chunk-file-v1_12`
  - `artifactFamily=generated-lazy`
  - `chunkId`
  - `chunkImportSpecifier`
  - `chunkArtifactPath`
  - `chunkRegistrySource`
  - `chunkFileResolver`
  - `chunkFileStatus`
  - `chunkFileBytes` / sha metadata。
- 相对 dependency 继续复用 v1.8/v1.11 stable staged resolver；不能为 chunk file 建一个只服务
  单个 fixture 的 hard-coded path。

验收要求：

- Preferred：至少一个真实 chunk file 被 request/response/stage。
- Acceptable：如果当前构建没有真实 chunk file，必须有 `chunkFileStatus=not-generated` 和
  `noChunkGeneratedReason`，并证明来自真实 generated-flow-imports no-op body或真实 stats/build metadata。

### 3. Chunk Execution and Side-Effect Proof

目标：

- 对找到的 real chunk file，host 必须 stage/import 并返回 `Promise` 给原始
  `window.Vaadin.Flow.loadOnDemand(chunkId)` caller。
- Trace 必须记录：
  - `loadOnDemandChunkFileTriggeredByFlowClient=true`
  - `loadOnDemandChunkFileStageObserved=true`
  - `loadOnDemandChunkFileExecuted=true|false`
  - `loadOnDemandChunkFileExecutionResult`
  - `loadOnDemandChunkFileCallbackResolved`
  - `loadOnDemandChunkFileSideEffectProof`
  - browser error / failure layer。
- 如果 chunk 执行没有 visible DOM 变化，必须至少提供一种 side-effect proof：
  - module import resolved；
  - Promise callback resolved；
  - custom element registry before/after；
  - module export/import result；
  - Flow client dependency continuation；
  - app-visible marker。

验收要求：

- Preferred：真实 UIDL chunk id -> real chunk file -> stage/import -> callback resolved。
- Acceptable：仍未执行成功，但 blocker 必须已经越过 v1.11 generated-flow-imports no-op 层，
  精确到 `chunk-file-not-generated`、`chunk-file-not-found`、`chunk-file-import-failed`、
  `chunk-callback-not-resolved` 或 `side-effect-unobservable`。

### 4. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_12-loadondemand-chunk-file`：只验证 v1.12 generated chunk-file delta。
- `--require-flow-ipc-v1_12`：累计阶段完成 gate，表示 v1.11 baseline + v1.12 delta。

规则：

- `--require-flow-ipc-v1_12` 不能只继承 v1.11 后直接返回成功。
- v1.12 delta 不能用 v1.9 diagnostic bridge、v1.10 ordinary `LoadMode.LAZY` dependency、
  v1.11 generated-flow-imports artifact mapping 或 direct staged import 代替。
- 如果 v1.12 以 blocker 形式完成，该 blocker 必须来自真实 generated-flow-imports body、
  real stats/build metadata 或真实 chunk file import attempt，不接受 synthetic-only probe。
- v1.8/v1.9/v1.11 historical gates 仍可单独验证，但 cumulative path 应把 v1.12 作为
  replacement evidence，而不是重新要求旧 no-op blocker 成功。

交付：

- 新增 `docs/traces/flow-ipc-v1_12.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.12 delta/cumulative gate。
- 保持 v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9/v1.10/v1.11 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 5. Runtime Resolver / Test Surface

目标：

- 将 v1.11 chunk bridge 扩展为 chunk-file aware resolver：
  - registry parser；
  - chunk artifact index；
  - stable module identity；
  - cache hit/miss；
  - cleanup ownership；
  - callback/result handling；
  - blocker classification。
- `FlowClientResourceSmokeMain` 或新增 test-source smoke 必须验证 v1.12 metadata，
  不能只依赖真实窗口 fixture。
- 如果需要让当前 smoke app 产生真实 chunk file，可以新增 focused Flow dependency 或
  build input，但必须仍通过 Vaadin Flow / Vaadin build contract 产生 generated-flow-imports
  chunk，不得写 repository-local fake chunk file 冒充 Vaadin 输出。
- `test-apps/flow-over-ipc-framework-app` 至少保持 v1.10 lazy/runtime validation，不因 v1.12
  focused smoke path 退化。

### 6. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/acceptance-tools.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`
- 如 framework app 行为变化，也更新对应 README。

文档必须明确：

- v1.11 的成功边界是 chunk id -> generated-flow-imports artifact mapping。
- v1.12 的目标是 chunk id -> real `./chunks/chunk-*.js` file mapping、execution 或 precise
  no-chunk-generated blocker。
- 如果 no-op table 是当前事实，说明它来自 Vaadin generated-flow-imports 输出，不是框架成功。
- production bundle owned runtime 和 dev-mode/Vite execution 仍不是本阶段成功标准，除非本阶段
  明确实现其中一部分并有独立 gate。

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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12-loadondemand-chunk-file docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12 docs/traces/flow-ipc-v1_12.ndjson
```

真实窗口 smoke 验证：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
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

- 不要求一次完成 production bundle owned runtime execution。
- 不要求一次完成 dev-mode/Vite execution。
- 不要求扩大到 Grid/ComboBox/Dialog/Upload 等 broader component parity。
- 不要求把 macOS Swift host 默认化。
- 不允许把 localhost compatibility mode、v1.9 diagnostic bridge、v1.10 ordinary
  `LoadMode.LAZY` dependency、v1.11 generated-flow-imports artifact mapping 或 repository-local
  fake chunk table 描述为 v1.12 chunk-file 成功。

## 完成标准

v1.12 完成时应满足：

- `docs/traces/flow-ipc-v1_12.ndjson` 存在并通过 v1.12 delta / cumulative inspector gate。
- Trace 明确区分 v1.9 diagnostic bridge、v1.10 ordinary lazy dependency、v1.11 generated
  artifact mapping 和 v1.12 generated chunk-file registry。
- 至少一个真实 UIDL chunk id 进入 real chunk-file registry path；如果没有 chunk file，必须有
  可审计 no-op / no-chunk-generated evidence。
- Chunk file request/stage/execute/callback/side-effect proof 或 precise blocker 被记录。
- 文档同步更新长期账本和 runtime 限制，不把 blocker 包装成最终成功。
- 所有验收命令中适用于当前平台的命令通过；如果某项因环境限制无法运行，必须在提交说明中记录。
- 每轮工作按适合粒度提交 Conventional Commit。
