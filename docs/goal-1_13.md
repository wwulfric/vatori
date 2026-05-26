# Goal 1.13: Production Bundle Owned Runtime Boundary

状态：已规划，尚未执行。

`docs/goal.md` 是长期愿景和完成度账本。v1.12 已经把 generated
`Flow.loadOnDemand(chunkId)` 的 chunk-file 方向收敛清楚：当前真实 Vaadin
`generated-flow-imports-*.js` 输出是 `Promise.resolve(0)` no-op table，没有
`./chunks/chunk-*.js` branch。因此 v1.13 不再把“当前 app/build 必须找到 generated
chunk file”作为主线目标；继续在同一个 no-op table 上硬推，只会制造假成功。

**核心目标：把 v1.10 的 production bundle `execution-attempt` 推进为有明确 owner 的
production bundle runtime boundary。runtime 必须能从真实 Vaadin dev-bundle / stats
metadata 中选择 production artifact，以独立于 focused component entry 的身份 stage/import，
记录 module identity、side effect、collision 和 cleanup 证据；如果仍不能安全执行，必须把
blocker 从 v1.10 的宽泛 `production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports`
收敛到更精确的 runtime ownership / side-effect / custom element registry / browser execution 层。**

## 为什么选择这个目标

v1.5-v1.12 已经证明：

- no-localhost staging resolver 能执行真实 classpath frontend artifact。
- TextField / Checkbox / Select 可以通过 focused dev-bundle entry 和 stable module identity
  运行。
- generated/lazy graph 可以 stage/import `generated-flow-imports-*.js` 及其 relative
  dependency。
- ordinary Flow `LoadMode.LAZY` dependency 可以由可见 Flow DependencyLoader 触发并执行。
- generated `Flow.loadOnDemand(chunkId)` 可以从真实 UIDL expression 触发，但当前 build
  没有真实 generated chunk file。

剩下最接近产品化 frontend/resource pipeline 的 blocker 是 production bundle ownership：
当前 framework 能验证并尝试 stage production `indexhtml` chunk，但还没有把它作为一个
由 runtime 拥有、可复用、可诊断、可清理的执行单元。没有这个 owner，component graph
会继续依赖 focused entry imports，production bundle / dev-bundle side effects 的边界也会
继续含糊。

## 继承边界

以下能力在 v1.13 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC runtime，核心 UI path 不通过 localhost HTTP。
- 可见 Flow client engine 继续消费 bootstrap、event patch、ordered push、resync、
  ordinary `LoadMode.LAZY` dependency 和 generated `Flow.loadOnDemand(chunkId)` trigger。
- v1.7 TextField graph execution 继续通过，旧 TextField shim 只能作为 legacy fallback。
- v1.8 generated/lazy relative dependency resolver 继续通过。
- v1.10 ordinary `LoadMode.LAZY` dependency path 继续通过。
- v1.11 real UIDL `Flow.loadOnDemand(chunkId)` trigger 继续通过。
- v1.12 generated-flow-imports no-op table parser 继续通过，并保留
  `chunkFileStatus=not-generated` 的真实证据。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync、
  WebView error reporting 和 framework app validation 不退化。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。

## 非目标

- 不用 repository-local fake chunk file 冒充 Vaadin generated chunk。
- 不把 v1.12 的 `chunkFileStatus=not-generated` 当成 production bundle 成功。
- 不把 focused component entry import 的副作用继续算作 production bundle owner 成功。
- 不要求本阶段完成 dev-mode/Vite 全量执行。
- 不要求本阶段完成 Windows/Linux host。
- 不要求完整 Vaadin production bundle parity；可以以更精确 blocker 完成，但必须比 v1.10
  的宽泛 blocker 更接近真实失败层。

## 实现范围

### 1. Production Bundle Runtime Owner

目标：

- 在 Java manifest / resource response 中显式声明 production bundle runtime owner：
  - `productionBundleRuntimeOwnerVersion=flow-ipc-production-owner-v1_13`
  - `productionBundleRuntimeOwner=flow-ipc-production-bundle-runtime`
  - `productionBundleRuntimeEntryArtifact`
  - `productionBundleRuntimeDependencyChain`
  - `productionBundleRuntimeSource=vaadin-dev-bundle-stats`
  - `productionBundleRuntimeSelectedBy=stats-entrypoint-or-focused-production-entry`
- 该 owner 必须独立于 `BigDecimalField`、`TextField`、`Checkbox`、`Select` 的 focused
  component importer；component importer 可以复用 owner 的 staged identity，但不能把自己的
  import 当成 owner 成功。
- Runtime owner 需要有明确生命周期：
  - created
  - staged
  - imported / import-failed
  - side-effect-observed / side-effect-unobservable
  - reused-by-component / not-reused
  - cleanup-owned。

验收要求：

- `FlowClientResourceSmokeMain` 或新增 focused smoke 验证 production owner metadata。
- 真实窗口 trace 中出现 production owner manifest / request / response / stage / DOM evidence。

### 2. Owner-Scoped Staging and Module Identity

目标：

- 扩展现有 stable resource-path Blob module identity，使 production bundle owner 拥有自己的
  identity namespace，例如：
  `production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`。
- 记录 cache hit/miss、owner identity key、dependency identity key、stage URL scheme、
  lifetime owner 和 cleanup strategy。
- Preferred：评估并实现最小 custom-scheme / file-backed resource path prototype，用于替代
  长生命周期 Blob URL。
- Acceptable：如果 custom scheme 超出阶段范围，继续使用 Blob URL，但必须记录为什么仍是
  temporary staging strategy，以及后续迁移条件。

验收要求：

- `resourceStage` trace 中能区分 `owner=production-bundle-runtime` 与
  `owner=component-focused-entry`。
- DOM diagnostics 记录 production owner identity、cache、cleanup 和 reuse 情况。

### 3. Production Side-Effect and Collision Diagnostics

目标：

- Host 在 import production bundle 前后记录：
  - `window.Vaadin` / `window.Vaadin.Flow` 状态；
  - custom element registry before/after；
  - 已定义 Vaadin tags；
  - import result / thrown browser error；
  - duplicate definition / already-defined collision；
  - side-effect marker 或 side-effect-unobservable reason。
- 如果 production chunk 会触发 duplicate custom element definition，不能吞掉错误；
  必须记录：
  - duplicate tag；
  - first definition owner；
  - second definition owner；
  - failure layer；
  - next resolution step。
- 如果 production import 成功但没有可观测副作用，必须记录
  `productionBundleRuntimeSideEffectProof=module-import-resolved-side-effect-unobservable`
  或更具体证据。

验收要求：

- Preferred：runtime owner import 成功，并给出至少一个 side-effect proof。
- Acceptable：runtime owner import 失败，但 blocker 精确到以下之一：
  - `production-owner-duplicate-custom-element`
  - `production-owner-browser-import-failed`
  - `production-owner-side-effect-unobservable`
  - `production-owner-global-collision`
  - `production-owner-runtime-contract-missing`

### 4. Component Resolver Integration

目标：

- TextField / Checkbox / Select focused dev-bundle entry 继续能工作。
- 如果 production owner 已 stage/import 某个 shared dependency，component resolver 应记录
  是否复用该 identity：
  - `componentProductionOwnerReused=true|false`
  - `componentProductionOwnerReuseReason`
  - `componentProductionOwnerDependencyPaths`
- 不能为了让 production owner 成功而牺牲现有组件事件链路。

验收要求：

- Smoke app 继续验证 BigDecimalField、TextField、Checkbox event -> Java listener -> UIDL
  patch -> visible DOM。
- Framework app 继续验证 BigDecimalField、Checkbox、Select、Button、list update 和 desktop
  capability call。

### 5. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_13-production-owner`：只验证 v1.13 production owner delta。
- `--require-flow-ipc-v1_13`：累计阶段完成 gate，表示 v1.12 baseline + v1.13 delta。

规则：

- v1.13 delta 不能用 v1.10 的 `execution-attempt` 直接冒充。
- v1.13 delta 不能用 component focused import 的成功冒充 production owner 成功。
- 如果以 blocker 完成，blocker 必须来自真实 production artifact import attempt 或真实
  runtime owner contract evidence。
- v1.10 production runtime gate 继续保留为历史阶段 gate，但 v1.13 应该提供更精确的 replacement
  evidence。

交付：

- 新增 `docs/traces/flow-ipc-v1_13.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.13 delta/cumulative gate。
- 保持 v1-v1.12 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

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

- v1.12 的 `chunk-file-not-generated` 是真实 Vaadin build 事实，不是生产 bundle成功。
- v1.13 的目标是 production bundle runtime ownership，不是 broad production parity。
- component focused imports、production owner、generated/lazy、ordinary Flow lazy dependency
  是四条不同证据链。
- Compatibility mode 仍是 fallback，不能作为 Flow IPC 成功证据。

## 验收命令

最小验收集合：

```bash
git diff --check
bash -n bin/vaadin-desktop
source /Users/bytedance/.sdkman/bin/sdkman-init.sh && mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
bash bin/flow-client-resource-smoke
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
```

Trace gate matrix：

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
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.13 可以用两种方式完成：

1. Preferred：production bundle owner 能 stage/import 真实 production artifact，记录 owner-scoped
   module identity、side-effect proof，并保持组件与 framework app 回归通过。
2. Acceptable：production bundle owner 仍不能安全执行，但 blocker 已经精确到 runtime owner 的
   具体失败层，并且提供 source artifact、stage identity、browser/native error、registry/collision
   diagnostics 和下一步拆解。

无论采用哪种方式，都必须新增 v1.13 trace fixture、strict inspector gate、文档更新和真实窗口
验证结果。
