# Goal 1.15: Production Owner Dependency Resolver Closure

状态：已规划，尚未执行。

`docs/goal.md` 是长期愿景和完成度账本。v1.14 已经把 production owner 从
`production-owner-runtime-contract-missing` 推进到真实 import attempt：Swift host 会在
component focused imports 之后，由 `owner=production-bundle-runtime` 对真实
stats-selected `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js` 发起受控
dynamic import，并记录 Vaadin global snapshot、custom element registry snapshot、
browser error 和 side-effect diagnostics。

v1.14 的真实结果仍不是 production owner import success。当前 blocker 已经更具体：

- `productionBundleRuntimeOwnerImportAttempted=true`
- `productionBundleRuntimeOwnerFailureLayer=production-owner-relative-import-unresolved`
- `productionBundleRuntimeOwnerImportResolvedDependencyPaths=vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js`
- `productionBundleRuntimeOwnerImportUnresolvedSpecifiers` 包含多个真实 production relative
  imports，例如 copilot chunks、`markdown-helpers-31I0A-hg.js`、`FlowBootstrap-*`、
  `FlowClient-*`、`generated-flow-imports-*` 等。
- Browser 同时报告 duplicate custom element error，但 v1.14 仍优先把 final blocker 分类为
  relative dependency unresolved，因为 owner dependency graph 尚未闭合。

**核心目标：把 production owner import 的 dependency resolver 从 focused single-dependency
rewrite 推进到可审计的 owner-scoped relative dependency graph closure。Preferred 是真实
stats-selected production owner artifact 的 relative imports 全部被解析、stage 和 rewrite，
owner import 继续向前执行并给出 side-effect proof。Acceptable 是所有存在于真实
dev-bundle build 目录中的 relative dependencies 都被解析并 stage，最终 blocker 从
`production-owner-relative-import-unresolved` 收敛到 custom element collision、browser import
failure、global collision、side-effect-unobservable，或明确的 missing-real-artifact 列表。**

## 为什么选择这个目标

v1.14 已经证明 owner import 真实发生，但它还没有足够的 dependency resolver 能力。继续扩大
Grid、ComboBox、Dialog、Upload 等组件覆盖，会把更多 production bundle side effects 叠在
focused component entry 上，短期看会热闹一点，长期会让 owner 边界更难拆。

v1.15 不要求完整 production parity，也不要求马上解决 duplicate custom element ordering。它只
要求把当前最靠前的失败层从“relative imports 未解析”推进到更下游的真实 runtime 层。完成后，
后续阶段才能有资格讨论 owner 先于 component imports 执行、custom scheme / file-backed
resolver、production side-effect ownership 或更宽组件覆盖。

## 继承边界

以下能力在 v1.15 中视为基线，不能回退：

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
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、
  repeated/list update、desktop capability、file transfer、push ordering、resync、
  WebView error reporting 和 framework app validation 不退化。
- `test-apps/flow-over-ipc-framework-app` 继续作为普通开发者项目形态的 validation app。

## 非目标

- 不用 repository-local fake chunk 或 fake production artifact 冒充 Vaadin build 输出。
- 不把 missing real artifact 包装成 success；如果真实 dev-bundle 中没有文件，必须记录
  artifact path、specifier、resolver root 和 missing classification。
- 不把 focused component entry import 的成功算作 production owner dependency closure。
- 不要求本阶段完成完整 production owner import success。
- 不要求本阶段完成 dev-mode/Vite execution。
- 不要求本阶段解决 generated-flow-imports no-op table / missing generated chunk file。
- 不要求 Windows/Linux host。
- 不为了 owner import 成功牺牲现有 component event chain。

## 实现范围

### 1. Owner-Scoped Relative Dependency Resolver

目标：

- 在 Swift WebView runtime 中为 `owner=production-bundle-runtime` 增加 recursive 或至少
  manifest-driven relative dependency resolver：
  - source artifact 仍来自真实
    `vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`；
  - resolver root 必须是该 artifact 所在的真实 dev-bundle build 目录；
  - relative specifier 例如 `./foo.js`、`./foo`、`./chunks/foo.js` 必须解析成真实 artifact
    path 或明确 missing；
  - resolved dependencies 使用 owner-scoped stable module identity stage；
  - rewrite 结果必须指向 staged dependency URL 或后续 custom-scheme/file-backed URL。
- 对 v1.14 已观测到的 unresolved specifiers 逐项分类：
  - `resolved-existing-artifact`
  - `missing-real-artifact`
  - `unsupported-extension`
  - `external-or-bare-specifier`
  - `deferred-dynamic-import`
- 记录 dependency graph summary：
  - total specifier count；
  - resolved count；
  - missing count；
  - staged count；
  - recursive depth；
  - root artifact；
  - resolver root；
  - module identity namespace；
  - dependency cache hit/miss。

验收要求：

- 真实窗口 trace 中出现
  `productionBundleRuntimeOwnerDependencyResolverVersion=flow-ipc-production-owner-deps-v1_15`。
- v1.14 的 unresolved relative specifiers 不能原样作为唯一 final blocker 继续出现。
- 如果仍有 unresolved，必须按 specifier/path 给出 missing-real-artifact 或 unsupported 分类。

### 2. Owner Import After Dependency Rewrite

目标：

- 在 dependency graph closure 后再次发起 owner import attempt：
  - `productionBundleRuntimeOwnerImportAttempted=true`
  - `productionBundleRuntimeOwnerDependencyGraphClosed=true|false`
  - `productionBundleRuntimeOwnerImportDependencyRewriteResult=closed|partial|missing-real-artifact`
  - `productionBundleRuntimeOwnerImportStageCount`
  - `productionBundleRuntimeOwnerImportResolvedDependencyPaths`
  - `productionBundleRuntimeOwnerImportMissingDependencyPaths`
- 如果 owner import 成功，记录 side-effect proof：
  - `window.Vaadin` / `window.Vaadin.Flow` before/after；
  - `window.Vaadin.Flow.loadOnDemand` owner / install source；
  - custom element registry before/after；
  - changed tags；
  - side-effect owner。
- 如果 owner import 失败，分类到更下游层：
  - `production-owner-duplicate-custom-element`
  - `production-owner-browser-import-failed`
  - `production-owner-global-collision`
  - `production-owner-side-effect-unobservable`
  - `production-owner-missing-real-artifact`

验收要求：

- Preferred：dependency graph closed，owner import succeeds or reaches observable side-effect proof。
- Acceptable：dependency graph closure is complete for all existing real artifacts, and final blocker is no
  longer generic `production-owner-relative-import-unresolved`。

### 3. Collision Ordering Evidence

目标：

- 保持 v1.14 的 `after-component-imports` probe，但新增至少一个 isolated/pre-component
  diagnostic mode，不要求默认启用：
  - `isolated-preflight`
  - `before-component-imports`
  - `after-component-imports`
- 记录 owner import phase 对 duplicate custom element error 的影响：
  - duplicate tag；
  - first definition owner；
  - second definition owner；
  - registry before/after；
  - import order；
  - whether component resolver could reuse owner。

验收要求：

- Trace 能判断 blocker 是 dependency resolution 仍未闭合，还是 component/owner ordering
  导致 custom element collision。
- 不能用禁用现有 component imports 的方式“制造” owner import success。

### 4. Java Metadata and Smoke Assertions

目标：

- 在 Java manifest / resource response 中声明 v1.15 owner dependency resolver contract：
  - `productionBundleRuntimeOwnerDependencyResolverVersion=flow-ipc-production-owner-deps-v1_15`
  - `productionBundleRuntimeOwnerDependencyResolverRoot`
  - `productionBundleRuntimeOwnerDependencyResolverPolicy`
  - `productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts`
  - `productionBundleRuntimeOwnerDependencyResolverAcceptableFailures`
- `FlowClientResourceSmokeMain` 或 focused smoke 断言这些 metadata 存在。
- 保留 v1.13 / v1.14 metadata，不能破坏历史 gates。

验收要求：

- `bash bin/flow-client-resource-smoke` 验证 v1.15 metadata。
- v1.14 fixture gate 继续通过。

### 5. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_15-production-owner-dependencies`：只验证 v1.15 production owner
  dependency resolver delta。
- `--require-flow-ipc-v1_15`：累计阶段完成 gate，表示 v1.14 baseline + v1.15 delta。

规则：

- v1.15 delta 必须证明 owner dependency resolver 真实处理 production owner artifact 的
  relative imports。
- v1.15 delta 不能只复用 v1.14 的 import-attempt evidence。
- v1.15 delta 不能用 component focused import 的成功冒充 dependency graph closure。
- 如果以 blocker 完成，blocker 必须来自 dependency graph closure 之后的真实 import attempt，
  或来自明确 missing-real-artifact 分类。
- `production-owner-runtime-contract-missing` 和未分类的
  `production-owner-relative-import-unresolved` 不能作为 v1.15 通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_15.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.15 delta/cumulative gate。
- 保持 v1-v1.14 fixture gates 继续可运行。
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

- v1.15 是 production owner dependency resolver closure，不是 broad production parity。
- v1.14 的 owner import attempt 是基线，但不足以证明 v1.15 完成。
- component focused imports、production owner import、production owner dependency closure、
  generated/lazy 和 ordinary Flow lazy dependency 是不同证据链。
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
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.15 可以用两种方式完成：

1. Preferred：production owner dependency graph closed，owner import 成功或产生可观测
   side-effect proof，且 smoke app / framework app 回归通过。
2. Acceptable：所有真实存在的 relative dependencies 都被 owner-scoped resolver stage/rewrite，
   最终 blocker 不再是未分类的 `production-owner-relative-import-unresolved`，而是明确的
   missing-real-artifact、duplicate custom element、browser import failure、global collision
   或 side-effect-unobservable。

无论采用哪种方式，都必须新增 v1.15 trace fixture、strict inspector gate、文档更新和真实窗口
验证结果。
