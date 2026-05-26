# Goal 1.10: Flow Lazy Dependency and Production Bundle Runtime Slice

`docs/goal.md` 是长期愿景和完成度账本。v1.9 已完成的是
`Flow.loadOnDemand` contract 的 blocker 前移：trace 已能说明当前 API 是由真实
`generated-flow-imports-*.js` direct staged import 安装，再由 diagnostic bridge 调用；
它不是最终 Flow client engine 自己触发的 generated/lazy path。

本阶段要比 v1.9 更大：不能只新增一个字段或 gate。v1.10 必须同时推进 **Flow lazy
dependency runtime** 和 **production bundle executable graph** 两条边界，其中 lazy
dependency path 至少要有一个真实执行成功，production bundle path 至少要进入可复现的
staged execution attempt 或明确成功。

**核心目标：让 no-localhost `dev-ipc` runtime 处理真实 Flow lazy dependency / production
bundle runtime 请求，而不是继续只依赖 direct staged probe、stats verified-only 或
repository-local fixture。**

## 继承边界

以下能力在 v1.10 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC smoke runtime，不通过 localhost HTTP URL 加载
  核心 UI path。
- Visible Flow client engine 继续负责 bootstrap、event patch、push 和 resync 的可见
  renderer。
- v1.7 TextField graph execution 继续通过：真实 `vaadin-text-field` graph 经 stable
  resource-path module identity 执行，`Vaadin TextField: Flow IPC typed` 事件链路继续可验证。
- v1.8 generated/lazy relative dependency resolver 继续通过：
  `generated-flow-imports-VW7hRaNw.js` 的 `./markdown-helpers-31I0A-hg.js` import 被
  rewrite 到 stable staged dependency，两个 artifact 均可执行。
- v1.9 loadOnDemand contract evidence 继续通过：API owner、install source、requested
  chunk、registry/mapping source、resolved artifact path、diagnostic bridge 和
  `frontendArtifactGeneratedLazyFlowClientTriggered=false` 都继续可追踪。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync 和
  WebView error-reporting baseline 继续可验证。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。
- 旧 TextField shim 仍只能作为 legacy fallback / regression guard，不能作为 v1.10
  成功证据。

## 本地 Vaadin 合同依据

制定 v1.10 时要优先参考本机 Vaadin 源码，而不是猜测：

- `flow-build-tools/src/main/java/com/vaadin/flow/server/frontend/AbstractUpdateImports.java`
  生成 `generated-flow-imports.js`，其中 `loadOnDemand(key)` 会按 chunk key 动态
  `import('./chunks/<chunk>.js')`，最后安装到
  `window.Vaadin.Flow.loadOnDemand`。
- `flow-server/src/test/java/com/vaadin/flow/server/communication/UidlWriterTest.java`
  证明 component dependencies 会在 UIDL 里出现
  `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` 形式的 lazy dependency。
- `flow-client/src/main/java/com/vaadin/client/DependencyLoader.java` 会先处理 eager/inline
  dependency，再 deferred 处理 `LoadMode.LAZY` dependencies。

v1.10 的实现应围绕这些真实合同做 no-localhost adapter，而不是新增一个
repository-local fake lazy API。

## 当前 blocker

截至 v1.9，已经证明：

- 真实 generated-flow-imports dev-bundle chunk 和 markdown helper 可以 stage + execute。
- `Flow.loadOnDemand` API owner / install source / requested chunk / registry source /
  mapping source 都能被 trace。
- v1.9 delta gate 能区分 diagnostic bridge 和 Flow-client-engine-triggered path。

仍未完成的是：

- WebView 中的 visible Flow client engine 还没有通过真实 UIDL lazy dependency 或
  DependencyLoader path 触发 no-localhost resource loading。
- `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` 这类 UIDL lazy expression 还没有
  被 runtime 解析、映射、stage、执行并追踪。
- `stats.json` 仍主要是 verified-only graph evidence；production bundle graph 没有形成
  可执行 artifact registry、dependency traversal、cache/cleanup 和 failure classification。
- 当前 `generatedLazyRequestedChunk=vatori-flow-ipc-generated-lazy-v1_9-probe` 是受控 probe
  key，不是来自真实 Flow component dependency chunk id。

## 实现范围

### 1. Flow Lazy Dependency Runtime Path

目标：

- 在 `dev-ipc` smoke 或 focused validation path 中制造至少一个真实 Flow lazy dependency：
  - 优先使用真实 component dependency / `UidlWriter` 产生的
    `return window.Vaadin.Flow.loadOnDemand('<chunkId>');` expression。
  - 可额外使用 `LoadMode.LAZY` JavaScript/CSS dependency 作为 DependencyLoader lazy
    path 的对照样本。
- WebView/host 必须能区分：
  - eager dependency
  - inline dependency
  - ordinary `LoadMode.LAZY` resource
  - generated/lazy `Flow.loadOnDemand(chunkId)` expression
  - diagnostic bridge。
- Lazy resource 请求必须走 IPC resource resolver，不允许回退到 localhost URL。
- `resourceRequest` / `resourceResponse` / `resourceStage` / `webview.dom` 必须记录：
  - `lazyDependencySource`
  - `lazyDependencyMode`
  - `lazyDependencyExpression`
  - `lazyDependencyChunkId`
  - `lazyDependencyResolvedPath`
  - `lazyDependencyTriggerOwner`
  - `lazyDependencyTriggeredByFlowClient`
  - success/failure layer 和 browser error。

验收要求：

- 至少一个真实 lazy dependency 经 visible Flow client engine 或 Flow DependencyLoader path
  触发，并在 no-localhost resolver 下执行成功。
- 如果 generated `Flow.loadOnDemand(chunkId)` 仍无法成功，必须把 blocker 收敛到
  `flow-ipc-lazy-dependency-v1_10` 或等价版本，并证明 blocker 来自真实 UIDL lazy
  expression，而不是 v1.9 diagnostic probe。

### 2. Production Bundle Artifact Registry

目标：

- 把 production bundle 从 `stats.json` verified-only 推进到可执行 registry：
  - 读取或解析 `vaadin-dev-bundle/config/stats.json` 中的 chunk / asset / dependency 信息。
  - 建立 logical id -> artifact path -> dependency graph -> execution policy 的映射。
  - 将至少一个 production bundle artifact 从 graph metadata 推进到 staged execution
    attempt。
- Registry 必须能解释为什么选择某个 artifact：
  - source stats entry
  - dependencyOf
  - export/import relation
  - module identity key
  - cache policy
  - cleanup policy
  - expected side effect。
- 继续避免重复加载 `indexhtml` 导致 custom element duplicate definition；如果必须复用
  production `indexhtml`，必须沿用 v1.7 stable resource-path module identity。

验收要求：

- 至少一个 production-bundle family artifact 不再只是 `stats.json verified-only`，而是进入
  `resourceStage` staged/execution-attempt/executed/execution-failed 之一。
- 如果不能执行成功，必须给出 `flow-ipc-production-runtime-v1_10` blocker，包含 stats
  source、selected artifact、dependency chain、module identity、browser/native error 和下一步。
- 不能把 v1.7 TextField focused entry 或 v1.8 generated-flow-imports chunk 直接重命名成
  production bundle 成功；必须证明它来自 production bundle registry 路径。

### 3. Runtime Resolver Abstraction

目标：

- 收敛当前 WebView/Swift host 中针对 component、generated/lazy、theme/static、
  production/dev resource 的散落 metadata，形成更明确的 frontend runtime resolver contract。
- Contract 不要求大抽象，但至少要把这些概念结构化：
  - logical request id
  - artifact family
  - source manifest
  - resolver
  - path mapping
  - dependency graph
  - stage URL / module identity
  - execution policy
  - cache hit/miss
  - cleanup ownership
  - blocker classification。
- `FlowClientResourceSmokeMain` 或新增 test-source smoke 必须能验证 v1.10 metadata，而不是
  只靠真实窗口手工 trace。

### 4. Validation App Surface

目标：

- `test-apps/flow-over-ipc-smoke-app` 继续作为 strict fixture source。
- `test-apps/flow-over-ipc-framework-app` 至少新增一个 lazy/runtime 相关验证点：
  - preferred：普通 Flow Java API 触发 lazy dependency 或新增一个更复杂组件路径。
  - acceptable：focused route/component 只为触发 lazy dependency，但必须仍像外部项目一样
    依赖 framework modules，不能写 host-specific JS。
- 新增验证点必须通过 visible Flow client engine -> IPC bridge -> Java listener ->
  UIDL patch -> visible DOM 或 resource side effect 证明。

### 5. Inspector Gate Semantics

新增或明确三类 gate：

- `--require-flow-ipc-v1_10-lazy-dependency`：只验证 v1.10 lazy dependency delta。
- `--require-flow-ipc-v1_10-production-runtime`：只验证 v1.10 production bundle runtime delta。
- `--require-flow-ipc-v1_10`：累计阶段完成 gate，表示 v1.9 baseline + 两个 v1.10 delta 都必须通过。

规则：

- `--require-flow-ipc-v1_10` 不能只继承 v1.9 后直接返回成功。
- 若 production path 使用 blocker 结果，该 blocker 必须是 v1.10 新 blocker，且由真实 staged
  execution attempt 产生。
- v1.6 仍是历史 blocker gate，不应重新成为 cumulative path 的前置条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_10.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.10 cumulative gate 和两个 delta gate。
- 保持 v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 6. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或新增 focused v1.10 protocol note
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/acceptance-tools.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`
- 如 framework app 行为变化，也更新对应 README。

文档必须明确：

- v1.9 diagnostic `Flow.loadOnDemand` bridge 与 v1.10 lazy dependency runtime path 的区别。
- 哪些 lazy dependencies 是由 Flow client / DependencyLoader 触发，哪些仍是 diagnostic。
- Production bundle artifact 是 graph verified-only、staged、execution-attempt、executed 还是
  execution-failed。
- v1.10 delta gates 与 cumulative gate 的关系。
- 如果仍有 blocker，它位于 UIDL expression、chunk id mapping、stats graph、module identity、
  browser execution、side effect integration 或 Flow client integration 的哪一层。

## 验收命令

最小验收集合：

```bash
git diff --check
bash bin/flow-client-resource-smoke
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

若新增 focused lazy dependency smoke command，必须加入本节并在文档中说明它与真实窗口
fixture 的关系。

## Commit 切分建议

本阶段不应压成一个小补丁。建议按以下可 review 单元提交：

1. `feat:` lazy dependency fixture / UIDL expression evidence。
2. `feat:` frontend runtime resolver contract / production bundle registry。
3. `test:` v1.10 inspector delta + cumulative gates 和 trace fixture。
4. `docs:` v1.10 runtime、protocol、roadmap 和 goal ledger 更新。

实际提交可按实现情况调整，但 commit message 必须继续使用 Conventional Commit 前缀。

## 明确非目标

- 不要求完整 production bundle parity。
- 不要求完整 dev-mode/Vite runtime。
- 不要求解决所有 generated/lazy component chunks。
- 不要求 broader component parity 全覆盖，例如 Grid、ComboBox、DatePicker、Dialog、Upload
  全部通过。
- 不要求 durable push queue、ack、reconnect 或 crash recovery。
- 不要求 Windows/Linux host。
- 不要求生产打包、签名、公证或自动更新。
- 不要求把 compatibility HTTP mode 设为默认或用它作为 Flow IPC 成功证据。

## 完成判定

v1.10 只有在以下条件同时满足时才算完成：

- lazy dependency delta gate 通过，且至少一个 lazy resource 是由真实 Flow client /
  DependencyLoader path 触发并通过 no-localhost resolver 成功执行。
- production runtime delta gate 通过，且至少一个 production-bundle artifact 进入
  staged execution attempt；若未执行成功，必须有 v1.10 blocker 证据。
- v1.9 的 `Flow.loadOnDemand` diagnostic bridge 仍被清楚标记，不能被误描述为最终成功。
- 所有历史 fixture gates 继续通过。
- 真实窗口 smoke app 和 framework app 回归继续通过，且使用 `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1`。
- `docs/traces/flow-ipc-v1_10.ndjson` 已生成并可通过 v1.10 gates。
- 相关文档同步更新，且 `docs/goal.md` 的长期未完成账本正确反映 v1.10 后仍未完成的边界。

## 完成记录

本阶段已按“两条边界同时推进”的形式完成：

- Flow lazy dependency runtime path：smoke app 和 framework app 都通过普通 Flow
  `@JavaScript(..., loadMode = LoadMode.LAZY)` 声明
  `context://vatori-flow-ipc-lazy-v1_10.js`。可见 Flow `DependencyLoader` 触发 lazy
  script 后，host/WebView bridge 将请求转为 IPC `resourceRequest`，Java sidecar 返回真实
  resource body，WebView stage 为 Blob module URL 并执行。trace 记录
  `flow-ipc-lazy-dependency-v1_10`、`flowLazyDependencyTriggeredByFlowClient=true`、
  `flowLazyDependencyExecuted=true` 和
  `flowLazyDependencyExecutionResult=executed:vatori-flow-ipc-lazy-v1_10.js`。
- Production bundle runtime path：`vaadin-dev-bundle/config/stats.json` 不再只是
  verified-only evidence；它被作为 runtime registry source，选择
  `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`，并通过 stable
  resource-path module identity 记录 staged `execution-attempt`。完整 production runtime
  ownership 仍是 blocker，版本为 `flow-ipc-production-runtime-v1_10`，failure layer 为
  `production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports`。
- Inspector 已新增三个 gate：
  `--require-flow-ipc-v1_10-lazy-dependency`、
  `--require-flow-ipc-v1_10-production-runtime` 和 `--require-flow-ipc-v1_10`。
- `docs/traces/flow-ipc-v1_10.ndjson` 已由真实窗口 smoke run 生成，并通过 v1.10 delta /
  cumulative gates。

仍未完成的长期边界：

- v1.9 的 generated `Flow.loadOnDemand` path 仍是 diagnostic bridge / API registry
  blocker，不应被 v1.10 的普通 `LoadMode.LAZY` dependency 成功混淆。
- Production bundle 只达到 staged execution attempt；还未拥有完整 side-effect runtime。
- Dev-mode/Vite execution、broader component parity 和 durable push/reconnect 仍属于后续
  goal。
