# Goal 1.16: Production Owner Import Ordering and Component Reuse

状态：已完成。

`docs/goal.md` 是长期愿景和完成度账本。v1.15 已完成 production owner dependency
resolver closure：它把 v1.14 的 focused single-dependency rewrite 推进为
owner-scoped relative dependency graph closure，使真实 stats-selected production owner
artifact 的 relative imports 能被解析、stage、rewrite，或被明确分类为真实缺失 artifact。

v1.16 只应在 v1.15 完成后执行。它承接 v1.15 的 dependency graph 证据，继续处理下一个最可能
的 runtime blocker：production owner import 与 component focused imports 的执行顺序、
custom element registry collision，以及 component resolver 是否能复用 owner-owned module
identity / side effects。

**核心目标：把 production owner 从 v1.14/v1.15 的 after-component import probe 推进到可审计
的 owner-first 或 isolated owner import path，并让 TextField / Checkbox / Select 等 component
focused resolver 明确复用或拒绝复用 owner side effects。Preferred 是 owner-first import 成功，
component imports 不再重复定义已有 Vaadin custom elements，并记录
`componentProductionOwnerReused=true`。Acceptable 是 owner-first / isolated import 真实运行后，
blocker 从 dependency resolver 层收敛到 custom element ordering、global collision、side-effect
ownership 或 unsupported Vaadin production owner contract 的具体失败层。**

完成结果采用 acceptable 路径：真实窗口 trace 记录
`flow-ipc-production-owner-ordering-v1_16` 和
`flow-ipc-component-owner-reuse-v1_16`。Swift host 在 component focused imports 之前执行
`before-component-imports` owner-first preflight，随后保留 after-component fallback import，
并在 `resourceStage` / `webview.dom` 中记录 observed phases、ordering result、
component reuse mode/reason、duplicate custom element browser error 和 final blocker。
当前 blocker 已从 dependency resolver / runtime contract 层推进到
`production-owner-duplicate-custom-element` 与 component reuse contract 层；这不是
production owner import success，也不是 broad production parity。

## 为什么选择这个目标

v1.13 把 production bundle runtime owner 身份拆出来；v1.14 证明 owner 真的能发起 import
attempt；v1.15 计划把 owner dependency graph 解析闭合。完成这些之后，如果仍然让 component
focused imports 先运行，再在后面尝试 owner import，就会继续把 production owner 限制在
diagnostic probe，无法走向产品化 runtime owner。

长期目标要求 production bundle / dev-mode bundle 有明确 staging、cache、cleanup 和 integrity
策略，也要求真实 Vaadin components 不依赖旧 shim 或局部副作用。v1.16 的价值是让 owner 和
component resolver 的关系变清楚：到底是 owner 先拥有 production side effects，然后 component
复用；还是当前 Vaadin artifact 形态不支持这种 owner-first path，需要另找 custom scheme、
file-backed resolver、Vaadin SPI 或 packaging-time rewrite。

## 继承边界

以下能力在 v1.16 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC runtime，核心 UI path 不通过 localhost HTTP。
- 可见 Flow client engine 继续消费 bootstrap、event patch、ordered push、resync、
  ordinary `LoadMode.LAZY` dependency 和 generated `Flow.loadOnDemand(chunkId)` trigger。
- v1.7 TextField graph execution 继续通过，旧 TextField shim 只能作为 legacy fallback。
- v1.8 generated/lazy relative dependency resolver 继续通过。
- v1.10 ordinary `LoadMode.LAZY` dependency path 继续通过。
- v1.11 real UIDL `Flow.loadOnDemand(chunkId)` trigger 继续通过。
- v1.12 generated-flow-imports no-op table parser 继续通过，并保留
  `chunkFileStatus=not-generated` 的真实证据。
- v1.13 production owner manifest / request / response / stage / DOM evidence 继续通过。
- v1.14 production owner import attempt 继续通过；不能回退到
  `production-owner-runtime-contract-missing`。
- v1.15 owner dependency resolver closure 证据继续通过；不能用未分类的
  `production-owner-relative-import-unresolved` 作为 v1.16 通过条件。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync、
  WebView error reporting 和 framework app validation 不退化。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。

## 非目标

- 不重复做 v1.15 的 dependency graph closure；如果 v1.15 没完成，本 goal 不应启动。
- 不用禁用 TextField / Checkbox / Select focused imports 的方式制造 owner import success。
- 不把 focused component entry import 的成功算作 owner-first import 成功。
- 不把 duplicate custom element error 吞掉或降级为 success。
- 不要求完整 Vaadin production bundle parity。
- 不要求本阶段完成 dev-mode/Vite execution。
- 不要求本阶段解决 generated-flow-imports no-op table / missing generated chunk file。
- 不要求 Windows/Linux host。
- 不为了 owner-first path 牺牲现有 component event chain。

## 实现范围

### 1. Owner Import Phase Control

目标：

- 在 Swift WebView runtime 中为 production owner import 增加明确 phase control：
  - `isolated-preflight`
  - `before-component-imports`
  - `after-component-imports`
- 默认 runtime 可以继续使用最保守的阶段，但 v1.16 trace 必须至少验证
  `before-component-imports` 或 `isolated-preflight` 中的一条真实路径。
- 每个 phase 都必须记录：
  - owner import phase；
  - root artifact；
  - dependency graph closure status；
  - stage count；
  - module identity namespace；
  - `window.Vaadin` / `window.Vaadin.Flow` before/after；
  - custom element registry before/after；
  - import result / browser error。

验收要求：

- 真实窗口 trace 中出现
  `productionBundleRuntimeOwnerImportOrderingVersion=flow-ipc-production-owner-ordering-v1_16`。
- Trace 不能只有 v1.14/v1.15 的 after-component import evidence；必须证明 owner-first 或
  isolated path 真实运行过。

### 2. Component Resolver Reuse

目标：

- 在 owner import 之后，TextField / Checkbox / Select focused component resolver 必须显式判断
  是否复用 owner side effects：
  - `componentProductionOwnerReused=true|false`
  - `componentProductionOwnerReuseVersion=flow-ipc-component-owner-reuse-v1_16`
  - `componentProductionOwnerReuseMode`
  - `componentProductionOwnerReuseReason`
  - `componentProductionOwnerOwnedTags`
  - `componentProductionOwnerSkippedDefinitions`
  - `componentProductionOwnerFallbackImports`
- 如果 owner 已注册某个 custom element，component focused entry 不能再次无保护定义同一 tag；
  必须走 guarded define、skip、reuse 或明确失败分类。
- 如果 component resolver 仍不能复用 owner，必须给出具体原因：
  - owner import failed before component import；
  - owner import succeeded but component class export is not reachable；
  - owner side effects are global but component entry still required for class-specific registration；
  - Vaadin generated production artifact does not expose reusable owner contract；
  - Blob/custom-scheme identity cannot safely merge owner and component lifetime。

验收要求：

- Preferred：至少 TextField 证明 owner-owned `vaadin-text-field` 可被 component resolver 复用，
  且真实 TextField event -> Java listener -> UIDL patch -> visible DOM 继续通过。
- Acceptable：component resolver 不能复用 owner，但 failure reason 精确到上面具体层之一。

### 3. Collision and Side-Effect Ownership Diagnostics

目标：

- 如果 owner-first import 或 component reuse 触发 custom element collision，必须记录：
  - duplicate tag；
  - first definition owner；
  - second definition owner；
  - registry before/after；
  - owner import phase；
  - component import order；
  - browser error name/message；
  - next resolution step。
- 如果 owner import 成功但没有可观测 side effects，必须记录：
  - `productionBundleRuntimeOwnerFailureLayer=production-owner-side-effect-unobservable`
  - `productionBundleRuntimeOwnerSideEffectProof`
  - `productionBundleRuntimeOwnerExpectedSideEffects`
  - `productionBundleRuntimeOwnerObservedSideEffects`
- 如果 owner import 成功且 component reuse 也成功，必须记录 side-effect owner：
  - `productionBundleRuntimeOwnerSideEffectOwner=production-bundle-runtime`
  - `componentProductionOwnerReused=true`
  - `componentProductionOwnerReuseMode=owner-owned-custom-elements`

验收要求：

- `production-owner-relative-import-unresolved` 不能作为 v1.16 final blocker。
- final blocker 必须来自 owner-first/import-order/component-reuse 层。

### 4. Java Metadata and Smoke Assertions

目标：

- 在 Java manifest / resource response 中声明 v1.16 owner ordering / reuse contract：
  - `productionBundleRuntimeOwnerImportOrderingVersion=flow-ipc-production-owner-ordering-v1_16`
  - `productionBundleRuntimeOwnerImportPhasePolicy`
  - `productionBundleRuntimeOwnerImportPhaseCandidates`
  - `componentProductionOwnerReuseVersion=flow-ipc-component-owner-reuse-v1_16`
  - `componentProductionOwnerReusePolicy`
  - `componentProductionOwnerReuseAcceptableFailures`
- `FlowClientResourceSmokeMain` 或 focused smoke 断言这些 metadata 存在。
- 保留 v1.13 / v1.14 / v1.15 metadata，不能破坏历史 gates。

验收要求：

- `bash bin/flow-client-resource-smoke` 验证 v1.16 metadata。
- v1.15 fixture gate 继续通过。

### 5. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_16-production-owner-ordering`：只验证 v1.16 production owner import
  ordering / component reuse delta。
- `--require-flow-ipc-v1_16`：累计阶段完成 gate，表示 v1.15 baseline + v1.16 delta。

规则：

- v1.16 delta 必须证明 owner-first 或 isolated import path 真实运行过。
- v1.16 delta 必须证明 component resolver reuse decision 真实发生。
- v1.16 delta 不能只复用 v1.14 import attempt 或 v1.15 dependency closure evidence。
- v1.16 delta 不能用 component focused import 的成功冒充 owner-first success。
- 如果以 blocker 完成，blocker 必须来自 owner import ordering、custom element collision、
  component reuse、global collision 或 side-effect ownership 层。
- `production-owner-runtime-contract-missing`、未分类的
  `production-owner-relative-import-unresolved` 不能作为 v1.16 通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_16.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.16 delta/cumulative gate。
- 保持 v1-v1.15 fixture gates 继续可运行。
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

- v1.16 是 production owner import ordering / component reuse boundary，不是 broad production
  parity。
- v1.15 dependency graph closure 是 v1.16 的前置条件。
- owner-first import、component focused import、component owner reuse、generated/lazy 和 ordinary
  Flow lazy dependency 是不同证据链。
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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14-production-owner-import docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14 docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15-production-owner-dependencies docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15 docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16-production-owner-ordering docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16 docs/traces/flow-ipc-v1_16.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.16 可以用两种方式完成：

1. Preferred：owner-first import 成功，component resolver 复用 owner-owned custom elements 或
   side effects，至少 TextField 真实事件链路继续通过，并记录
   `componentProductionOwnerReused=true`。
2. Acceptable：owner-first / isolated import 和 component reuse decision 都真实运行，但最终
   blocker 收敛到 custom element ordering、global collision、side-effect-unobservable、
   unsupported Vaadin production owner contract 或 component reuse contract missing 的具体层。

无论采用哪种方式，都必须新增 v1.16 trace fixture、strict inspector gate、文档更新和真实窗口
验证结果。
