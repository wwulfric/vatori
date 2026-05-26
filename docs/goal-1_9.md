# Goal 1.9: Flow-Client-Triggered Generated/Lazy Loading Contract

`docs/goal.md` 是长期愿景和完成度账本。v1.8 已完成的是 generated/lazy
staged graph 的 relative dependency resolver：真实 Vaadin dev-bundle
`generated-flow-imports-VW7hRaNw.js` 和 `markdown-helpers-31I0A-hg.js` 能通过
no-localhost staging resolver 执行，`./markdown-helpers-31I0A-hg.js` 不再是最终
blocker。

但 v1.8 的成功证据仍是 **direct staged import probe**：它证明真实 artifact body 和
relative dependency resolver 可以运行，但还没有证明 generated/lazy loading 是由可见
Flow client engine 通过真实 `Flow.loadOnDemand` contract 触发。

**核心目标：把 v1.8 的 direct staged generated/lazy import probe 推进为真实
Flow-client-triggered `Flow.loadOnDemand` path，或把 blocker 收敛到一个明确的
Vaadin lazy-loading API / bootstrap registry / client-engine integration 层。**

## 继承边界

以下能力在 v1.9 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC smoke runtime，不通过 localhost HTTP URL
  加载核心 UI path。
- Visible Flow client engine 继续负责 bootstrap、event patch、push 和 resync 的可见
  renderer。
- v1.7 TextField graph execution 继续通过：真实 `vaadin-text-field` graph 通过 stable
  resource-path module identity 执行，`Vaadin TextField: Flow IPC typed` 事件链路继续可验证。
- v1.8 generated/lazy relative dependency resolver 继续通过：
  `generated-flow-imports-VW7hRaNw.js` 的 `./markdown-helpers-31I0A-hg.js` import 能被
  rewrite 到 stable staged dependency。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync 和
  WebView error-reporting baseline 继续可验证。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。
- 旧 TextField shim 仍只能作为 legacy fallback / regression guard，不能作为 v1.9
  成功证据。
- `Flow.loadOnDemand` 不能被 repository-local 假模块或 inline shim 冒充；若需要 bridge，
  必须标记为 diagnostic bridge，不能计入最终成功。

## 当前 blocker

截至 v1.8，trace 已经证明：

- generated/lazy manifest metadata 存在。
- `generated-flow-imports-VW7hRaNw.js` 和 `markdown-helpers-31I0A-hg.js` 都能 stage。
- relative import 被 rewrite 到 stable staged module identity。
- `resourceStage` / `webview.dom` 记录
  `flow-ipc-generated-lazy-resolver-v1_8`、
  `frontendArtifactGeneratedLazyExecuted=true` 和
  `frontendArtifactGeneratedLazyLoadOnDemandResult=api-installed-by-staged-generated-flow-imports`。

仍未完成的是：

- 触发来源仍是 direct staged import probe，不是 Flow client engine 自己触发的 lazy path。
- `Flow.loadOnDemand` 的真实调用入口、参数 contract、module registry、chunk id / path
  映射、错误传播和生命周期还没有形成稳定 IPC contract。
- Production bundle execution 和 dev-mode/Vite execution 仍是明确 blocker，不计入 v1.9。

v1.9 要么证明真实 Flow client lazy-loading path 可以经 no-localhost IPC resolver 工作，
要么把 blocker 从“direct staged import probe”收敛为更具体的层级，例如：

- `Flow.loadOnDemand` API surface 未在当前 bootstrap/client engine 下暴露。
- API 暴露，但缺少 route/component/lazy chunk registry。
- registry 存在，但 chunk id 到 artifact path 的映射不完整。
- artifact path 可解析，但 client engine 不接受当前 staged module URL / module identity。
- execution 成功，但 side effect 没有接入可见 Flow client state。

## 实现范围

### 1. Flow Lazy API Reconnaissance

目标：

- 从当前 `FlowBootstrap.js`、`FlowClient.js`、`Flow.js`、`generated-flow-imports-*.js`
  和 Vaadin dev-bundle metadata 中梳理真实 lazy-loading API surface。
- 明确 `window.Vaadin.Flow.loadOnDemand` 或等价入口在当前 runtime 中何时安装、谁安装、
  需要什么参数、如何回调、如何报错。
- Trace 必须记录：
  - API owner / install source
  - trigger source
  - requested chunk id / module id
  - resolved artifact path
  - registry / mapping source
  - success / failure layer
  - browser error 或 Flow client error。

验收证据：

- `resourceRequest` / `resourceResponse` 或 `resourceStage` 中能看出 lazy API 到 artifact
  resolver 的映射过程。
- `webview.dom` 中能区分 direct probe、diagnostic bridge 和 Flow-client-triggered path。

### 2. Flow-Client-Triggered Lazy Execution

目标：

- 尽量通过真实 Flow client engine 可接受的路径触发 generated/lazy loading。
- 如果必须使用受控 probe，它必须调用真实 Flow client lazy API，而不是直接 `import()`
  真实 generated/lazy artifact。
- 至少一个真实 generated/lazy artifact body 仍必须被执行，不能退回到 verified-only。
- 继续复用 v1.8 stable resource-path module identity 和 relative dependency resolver。

可接受的两种结果：

1. **执行成功**
   - Flow client engine 或其公开 lazy API 触发 generated/lazy artifact loading。
   - Trace 记录 `triggerSource=flow-client-engine` 或等价字段。
   - DOM 记录 `frontendArtifactGeneratedLazyFlowClientTriggered=true`。
   - `frontendArtifactGeneratedLazyExecuted=true` 继续成立。
2. **blocker 前移**
   - Direct staged import 不再是唯一证据。
   - Trace 记录 `blockerVersion=flow-ipc-loadondemand-v1_9` 或等价字段。
   - `failureLayer` 必须是具体 API / registry / mapping / client integration 层，不能仍是
     unresolved relative import 或 generic direct-probe boundary。

### 3. Resource Resolver Contract

目标：

- 把 generated/lazy resolver 从“能 stage 某个 chunk”推进为“能服务 lazy API 的 artifact
  lookup”。
- 增加或明确 lazy artifact 的 mapping metadata：
  - logical chunk id / module specifier
  - artifact family
  - dependency graph
  - relative imports
  - execution policy
  - module identity key
  - cache hit/miss
  - cleanup policy
  - blocker classification。
- 如果当前 Vaadin metadata 不足，记录缺口并提供下一步所需 metadata source。

验收证据：

- `FlowClientResourceSmokeMain` 或等价 test-source smoke 能验证 v1.9 lazy metadata。
- `resourceStage` 能记录 Flow-client-triggered lazy request 与 staged artifact 的关联。

### 4. Inspector Gate Semantics

目标：

- 避免把“阶段完成态 cumulative gate”和“本阶段 delta gate”混在一起。
- 新增或明确两类 gate：
  - `--require-flow-ipc-v1_9`：累计阶段完成 gate，表示 v1.8 baseline + v1.9 delta 都必须通过。
  - `--require-flow-ipc-v1_9-loadondemand` 或等价命名：只验证 v1.9 lazy-loading delta，
    便于 review 新增能力。
- `--require-flow-ipc-v1_9` 不应继承历史 blocker-only gate，除非该 blocker 仍是当前成功路径的一部分。
- `DesktopTraceInspector.AcceptanceGate` 必须在代码和注释里体现 cumulative / delta 的区别。

交付：

- 新增 `docs/traces/flow-ipc-v1_9.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.9 cumulative gate 和 v1.9 delta gate。
- 保持 v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 5. Verification Apps

目标：

- `test-apps/flow-over-ipc-smoke-app` 继续作为 strict fixture source。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者形态验证。
- 只有当真实 lazy trigger 需要隔离更小的 Flow route/component 时，才新增 focused validation app。

验证 app 必须保持：

- 普通 Flow Java API。
- Button baseline。
- BigDecimalField baseline。
- TextField baseline。
- Checkbox 和 Select baseline。
- Repeated/list update baseline。
- 至少一个 manifest-gated native capability call。

### 6. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或新增 focused v1.9 protocol note
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/acceptance-tools.md`

文档必须明确：

- v1.8 direct staged import probe 与 v1.9 Flow-client-triggered lazy path 的区别。
- `Flow.loadOnDemand` 当前是被真实 Flow client 触发、被 diagnostic bridge 触发，还是仍是
  blocker。
- v1.9 cumulative gate 与 v1.9 delta gate 的区别。
- 哪些 generated/lazy artifacts 已执行，哪些仍只是 verified-only。
- 如果仍有 blocker，它已经进入哪个 API / registry / mapping / integration 层。

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

## 明确非目标

- 不要求完整 production bundle execution。
- 不要求完整 dev-mode/Vite runtime。
- 不要求 broader component parity，例如 Grid、ComboBox、DatePicker、Dialog、Upload。
- 不要求 durable push queue、ack、reconnect 或 crash recovery。
- 不要求 Windows/Linux host。
- 不要求生产打包、签名、公证或自动更新。
- 不要求把 compatibility HTTP mode 设为默认或用它作为 Flow IPC 成功证据。

这些仍保留在 `docs/goal.md` 的长期未完成账本中。

## 完成记录

本阶段以“blocker 前移”形式完成：v1.8 的 direct staged import 仍负责执行真实
`generated-flow-imports-VW7hRaNw.js` 和 `markdown-helpers-31I0A-hg.js`，但 v1.9 trace
不再把 direct import 当成最终 lazy-loading 成功。新增 metadata 和 gate 记录：

- `generatedLazyLoadOnDemandContractVersion=flow-ipc-loadondemand-v1_9`
- API owner / install source / requested chunk / registry source / mapping source / resolved artifact path
- `frontendArtifactGeneratedLazyDiagnosticBridge=true`
- `frontendArtifactGeneratedLazyFlowClientTriggered=false`
- `blockerVersion=flow-ipc-loadondemand-v1_9`
- `failureLayer=loadondemand-api-installed-by-generated-imports-not-flow-client-triggered`

`--require-flow-ipc-v1_9-loadondemand` 是本阶段 delta gate；
`--require-flow-ipc-v1_9` 是 v1.8 baseline + v1.9 delta 的 cumulative gate。
