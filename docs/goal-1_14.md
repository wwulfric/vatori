# Goal 1.14: Production Owner Import and Side-Effect Boundary

状态：已完成。

`docs/goal.md` 是长期愿景和完成度账本。v1.13 已经把 production
bundle runtime 从 v1.10 的宽泛 `execution-attempt` 推进为明确 owner contract：
真实 stats-selected `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`
artifact 会以 `owner=production-bundle-runtime` stage，并记录 owner module
identity、cache/cleanup、registry snapshot 和 `componentProductionOwnerReused=false`。

但 v1.13 仍停在 `production-owner-runtime-contract-missing`：owner 还没有真正
import production artifact，也没有证明 production chunk 的 side effects 可以由 owner
安全拥有。v1.14 的目标就是把这个边界再往前推一层。

**核心目标：让 production bundle runtime owner 对真实 stats-selected production
artifact 发起受控 import / side-effect probe。Preferred 是 owner import 成功，并记录
可观测 side-effect proof；Acceptable 是 import 真实发生但失败，且 blocker 从 v1.13 的
`production-owner-runtime-contract-missing` 收敛到 browser import、relative dependency、
custom element registry、global collision 或 side-effect-unobservable 的具体失败层。**

## 完成结果

v1.14 以 acceptable blocker 路径完成：production bundle runtime owner 已对真实
stats-selected `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js` artifact 发起
受控 import attempt，而不是继续停在 v1.13 的 owner stage / contract 证据。

当前真实窗口 trace 记录：

- `productionBundleRuntimeOwnerImportAttempted=true`
- `productionBundleRuntimeOwnerImportVersion=flow-ipc-production-owner-import-v1_14`
- `productionBundleRuntimeOwnerImportPolicy=controlled-owner-dynamic-import-after-component-graph-v1_14`
- `productionBundleRuntimeOwnerImportModuleIdentity=production-owner-import-v1_14:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`
- `productionBundleRuntimeOwnerImportDependencyRewriteResult=unresolved:...`
- `productionBundleRuntimeOwnerImportResolvedDependencyPaths=vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js`
- `productionBundleRuntimeOwnerFailureLayer=production-owner-relative-import-unresolved`
- `productionBundleRuntimeOwnerBrowserError=Cannot define multiple custom elements with the same tag name`
- `productionBundleRuntimeOwnerImportBrowserErrorName=NotSupportedError`
- `componentProductionOwnerReused=false`

因此 v1.14 不是完整 production bundle parity，也不是 production owner import success。
它完成的是 owner import / side-effect boundary：v1.13 的
`production-owner-runtime-contract-missing` 已被替换为真实 import attempt 产生的更具体
relative dependency / browser collision blocker，后续阶段可以继续拆 owner dependency
resolver、custom element collision ordering 或 custom-scheme/file-backed resolver。

## 为什么选择这个目标

长期愿景要求 frontend/resource pipeline 最终能产品化。v1.8-v1.12 已经把 generated/lazy
链路推进到真实 Vaadin artifact 和真实 no-op table 证据；v1.13 已经把 production owner
身份拆出来。此时继续扩大组件覆盖当然有价值，但 production bundle owner 仍然不 import，
会让后续 Grid、ComboBox、Dialog、Upload 等复杂组件继续依赖 focused component entry
的局部副作用，架构边界会变得越来越混。

v1.14 不要求完整 production bundle parity，也不要求 dev-mode/Vite parity。它只要求把
production owner 从“声明和 stage”推进到“真实 import 或真实 import 失败证据”。这一步小心
但关键：它会告诉后续阶段应该修 module identity、relative import rewrite、global owner、
custom element registry ordering，还是换 custom scheme / file-backed resolver。

## 继承边界

以下能力在 v1.14 中视为基线，不能回退：

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
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync、
  WebView error reporting 和 framework app validation 不退化。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。

## 非目标

- 不用 repository-local fake production artifact 冒充 Vaadin dev-bundle production chunk。
- 不把 v1.13 的 owner stage success 继续当作 v1.14 import success。
- 不把 focused component entry import 的成功算作 production owner import 成功。
- 不要求完整 Vaadin production bundle parity。
- 不要求本阶段完成 dev-mode/Vite execution。
- 不要求本阶段解决 v1.12 的 `chunkFileStatus=not-generated`。
- 不要求 Windows/Linux host。
- 不为了让 production owner import 成功而牺牲现有组件事件链路。

## 实现范围

### 1. Owner Import Attempt

目标：

- 在 Swift WebView runtime 中为 production owner 增加显式 import attempt：
  - source artifact 仍来自真实
    `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`；
  - owner identity 继续使用 v1.13 namespace，例如
    `production-owner-v1_14:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`
    或明确记录 v1.13 identity 复用规则；
  - import attempt 必须由 `owner=production-bundle-runtime` 发起；
  - import attempt 不能由 TextField / Checkbox / Select focused entry 代替。
- 在 import 前执行 preflight，记录：
  - artifact path / bytes / sha256；
  - unresolved relative imports；
  - resolved dependency paths；
  - owner import phase：`before-component-imports`、`after-component-imports`、
    `isolated-preflight` 或实际采用的阶段；
  - `window.Vaadin` / `window.Vaadin.Flow` 初始状态；
  - custom element registry before snapshot。
- 如果 production artifact 需要 relative dependency rewrite，至少对当前真实 dependency
  graph 做 focused rewrite 或给出具体 unresolved specifier blocker。

验收要求：

- 真实窗口 trace 中出现 `productionBundleRuntimeOwnerImportAttempted=true`。
- `resourceStage` 或新的等价 trace 明确记录 import attempt owner、stage URL、module
  identity、source artifact、dependency rewrite 结果和 execution result。
- `production-owner-runtime-contract-missing` 不能作为 v1.14 的最终 delta blocker。

### 2. Side-Effect and Collision Diagnostics

目标：

- Host 在 owner import 前后记录：
  - `window.Vaadin` 是否存在；
  - `window.Vaadin.Flow` 是否存在；
  - `window.Vaadin.Flow.loadOnDemand` owner / install source；
  - custom element registry before/after；
  - 已定义 Vaadin tags；
  - import resolved / rejected；
  - browser error name/message/stack summary；
  - side-effect proof 或 side-effect-unobservable reason。
- 如果 import 成功但没有可观测副作用，记录：
  - `productionBundleRuntimeOwnerFailureLayer=production-owner-side-effect-unobservable`
  - `productionBundleRuntimeOwnerSideEffectProof=module-import-resolved-side-effect-unobservable`
  - next resolution step。
- 如果 import 失败，分类到更具体层：
  - `production-owner-relative-import-unresolved`
  - `production-owner-browser-import-failed`
  - `production-owner-duplicate-custom-element`
  - `production-owner-global-collision`
  - `production-owner-side-effect-unobservable`

验收要求：

- Preferred：`productionBundleRuntimeOwnerImported=true`，且至少一个 side-effect proof 为
  true 或带有可验证文本。
- Acceptable：`productionBundleRuntimeOwnerImported=false`，但
  `productionBundleRuntimeOwnerImportAttempted=true`，并且 failure layer 是上述具体失败层之一。

### 3. Component Resolver Reuse Contract

目标：

- TextField / Checkbox / Select focused dev-bundle entry 继续可执行。
- 如果 owner import 成功或部分成功，component resolver 必须记录是否复用 owner identity：
  - `componentProductionOwnerReused=true|false`
  - `componentProductionOwnerReuseReason`
  - `componentProductionOwnerDependencyPaths`
  - `componentProductionOwnerImportOrder`
- 如果 component resolver 仍不能复用 owner import，必须给出具体原因，例如：
  - owner import failed before component import；
  - owner import succeeded but exports are not component-class-specific；
  - custom element registration must remain guarded in focused entry；
  - Blob identity cannot safely merge owner and component lifetime。

验收要求：

- Smoke app 继续验证 BigDecimalField、TextField、Checkbox event -> Java listener ->
  UIDL patch -> visible DOM。
- Framework app 继续验证 BigDecimalField、Checkbox、Select、Button、list update 和 desktop
  capability call。
- v1.14 trace 不能把 component focused import 的结果冒充 owner import side effect。

### 4. Java Metadata and Smoke Assertions

目标：

- 在 Java manifest / resource response 中声明 v1.14 owner import contract：
  - `productionBundleRuntimeOwnerImportVersion=flow-ipc-production-owner-import-v1_14`
  - `productionBundleRuntimeOwnerImportPolicy`
  - `productionBundleRuntimeOwnerImportArtifact`
  - `productionBundleRuntimeOwnerImportExpectedSideEffects`
  - `productionBundleRuntimeOwnerImportAcceptableFailures`
- `FlowClientResourceSmokeMain` 或 focused smoke 断言这些 metadata 存在。
- 保留 v1.13 metadata，不能破坏 v1.13 gates。

验收要求：

- `bash bin/flow-client-resource-smoke` 验证 v1.14 metadata。
- v1.13 fixture gate 继续通过。

### 5. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_14-production-owner-import`：只验证 v1.14 production owner import delta。
- `--require-flow-ipc-v1_14`：累计阶段完成 gate，表示 v1.13 baseline + v1.14 delta。

规则：

- v1.14 delta 必须有真实 import attempt 或真实 browser import failure evidence。
- v1.14 delta 不能用 v1.13 `resourceStage` owner contract 直接冒充。
- v1.14 delta 不能用 component focused import 的成功冒充。
- 如果以 blocker 完成，blocker 必须来自真实 production artifact import attempt 的执行结果。
- `production-owner-runtime-contract-missing` 只能作为 v1.13 历史 blocker，不能作为 v1.14
  通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_14.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.14 delta/cumulative gate。
- 保持 v1-v1.13 fixture gates 继续可运行。
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

- v1.14 是 production owner import / side-effect boundary，不是 broad production parity。
- v1.13 的 owner contract 是基线，但不再足够证明 v1.14 完成。
- component focused imports、production owner import、generated/lazy、ordinary Flow lazy
  dependency 是四条不同证据链。
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
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.14 可以用两种方式完成：

1. Preferred：production owner import 成功，记录 owner-scoped module identity、real import
   result、side-effect proof，并保持 smoke app / framework app 回归通过。
2. Acceptable：production owner import 失败，但 failure layer 已经从
   `production-owner-runtime-contract-missing` 推进到真实 import attempt 的具体失败层，并且
   提供 source artifact、stage identity、dependency rewrite、browser error、registry/collision
   diagnostics 和下一步拆解。

无论采用哪种方式，都必须新增 v1.14 trace fixture、strict inspector gate、文档更新和真实窗口
验证结果。
