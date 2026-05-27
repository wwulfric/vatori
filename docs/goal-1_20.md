# Goal 1.20: Production Owner-Owned Runtime Reuse Path

状态：已规划，正在执行。

`docs/goal.md` 是长期愿景和完成度账本。v1.19 已经把 production owner custom-scheme
import 之后的 `module import resolved` 与 `side effect satisfied` 拆开，并明确记录当前
component resolver 仍走 `focused-fallback-after-owner-side-effect-check`。v1.20 承接这个
结果，目标是把 production owner 从“可审计 fallback decision”推进到“owner-first runtime
reuse attempt”，让 TextField / Checkbox / Select 的 focused component path 能明确复用或拒绝
复用 owner-owned side effects。

**核心目标：让 production owner 在 focused component imports 之前获得一次真实
owner-first custom-scheme execution / side-effect opportunity；如果 expected tags/globals
满足，component resolver 必须复用 owner-owned runtime；如果仍不满足，blocker 必须收敛到
v1.20 owner-owned runtime contract 层，不能退回 v1.19 的单纯 after-check fallback。**

## 为什么选择这个目标

v1.19 的价值是避免误判：module import resolve 不等于 production owner side-effect success。
但它仍然没有让 production owner 成为组件 runtime 的实际来源。当前 TextField / Checkbox /
Select 依旧靠 focused component entry 成功，production owner 只是在旁边提供可审计证据。

要让 frontend/resource pipeline 接近基本闭环，需要至少证明一条 resource graph 的 ownership：

- owner root 和 dependency 由 host-owned custom scheme 提供；
- owner import 在合适时序运行；
- owner side effects 可被观察；
- component resolver 可以复用 owner-provided tags/globals，或以明确理由拒绝复用；
- fallback 仍可存在，但不能被包装成 owner success。

v1.20 因此优先处理 production owner ownership，而不是扩大组件清单。组件覆盖如果继续依赖
focused fallback，会让 pipeline 看起来更大，但没有让最终闭环更真。

## 继承边界

以下能力在 v1.20 中视为基线，不能回退：

- v1.18 production owner root 和至少一个 resolved dependency 继续通过
  `vatori-resource://flow-ipc/production-owner/...` file-backed custom-scheme URL stage/import。
- v1.19 继续区分 `productionBundleRuntimeOwnerModuleImported` 和
  `productionBundleRuntimeOwnerSideEffectSatisfied`，不能把前者单独算作 success。
- v1.19 `focused-fallback-after-owner-side-effect-check` 仍可作为 fallback，但不能作为 v1.20
  success。
- v1.7 TextField、v1.8 generated/lazy、v1.10 ordinary lazy dependency、v1.11 real UIDL
  `Flow.loadOnDemand(chunkId)` trigger、v1.12 no-op chunk-file proof、v1.13-v1.18 owner
  graph metadata 和 v1.19 side-effect diagnostics 继续通过。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、list update、
  desktop capability、file transfer、push ordering、resync、framework app validation 不退化。
- Legacy TextField shim 仍只能作为旧路径回归保护，不能计入 v1.20 成功。

## 非目标

- 不要求本阶段完成完整 production bundle / dev-mode / Vite parity。
- 不要求本阶段生成真实 `./chunks/chunk-*.js` lazy chunk file。
- 不要求移除 focused component fallback；如果 owner-owned runtime 不满足，fallback 可以继续用于
  保持 smoke app 行为，但必须被明确标记为 fallback。
- 不把 localhost HTTP、`file://` 直读、Blob root fallback 或 compatibility host 作为 v1.20
  成功证据。
- 不把 `productionBundleRuntimeOwnerModuleImported=true`、focused component import success、
  或 pre-existing custom element registry state 单独视为 owner reuse success。

## 实现范围

### 1. Owner-Owned Runtime Metadata

Java manifest / resource response 增加 v1.20 metadata：

- `productionBundleRuntimeOwnerOwnedRuntimeVersion=flow-ipc-production-owner-owned-runtime-v1_20`
- `productionBundleRuntimeOwnerOwnedRuntimePolicy=owner-first-custom-scheme-runtime-reuse-v1_20`
- `productionBundleRuntimeOwnerOwnedRuntimePhase=before-component-imports`
- `productionBundleRuntimeOwnerOwnedRuntimeExpectedTags=vaadin-text-field,vaadin-checkbox,vaadin-select`
- `productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals=window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand`
- `productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback=true`
- `productionBundleRuntimeOwnerOwnedRuntimeAcceptableFailures=production-owner-owned-side-effect-unobservable,production-owner-owned-duplicate-custom-element,production-owner-owned-missing-real-artifact,production-owner-owned-browser-import-failed,production-owner-owned-unsupported-vaadin-contract`
- `componentProductionOwnerReusePolicyVersionV1_20=flow-ipc-component-owner-reuse-policy-v1_20`
- `componentProductionOwnerReusePolicyV1_20=owner-first-then-focused-fallback-v1_20`

验收要求：

- `flow-client-resource-smoke` 断言 manifest、production runtime response、至少一个 owner
  dependency response 带 v1.20 metadata。
- v1.13-v1.19 metadata 继续保留，历史 gates 不退化。

### 2. Owner-First Runtime Attempt

Swift host 在 focused TextField / Checkbox / Select component imports 之前执行 owner-first
runtime attempt：

- 使用 v1.18 的 production owner custom-scheme root/dependency URLs；
- 在 attempt 前记录 custom element registry 和 Vaadin globals；
- 发起 owner root import；
- 在 attempt 后记录 registry/global diff；
- 记录 side-effect source 是否来自 owner-first phase，而不是 component fallback phase；
- 明确记录是否存在 duplicate custom element、missing artifact、browser import failure 或
  unsupported Vaadin runtime contract。

如果 owner-first path 满足 expected tags/globals：

- `productionBundleRuntimeOwnerOwnedRuntimeSatisfied=true`
- `componentProductionOwnerReuseDecision=owner-reused`
- focused component resolver 对已由 owner 提供的 tags 不重复 import，或明确记录 no-op reuse。

如果 owner-first path 不满足：

- `productionBundleRuntimeOwnerOwnedRuntimeSatisfied=false`
- `productionBundleRuntimeOwnerOwnedRuntimeFailureLayer=<v1.20 acceptable failure>`
- focused fallback 继续可运行，但必须记录
  `componentProductionOwnerReuseDecision=focused-fallback-after-owner-owned-runtime-attempt`
  和具体 reason。

### 3. Component Resolver Reuse Contract

TextField / Checkbox / Select focused resolver 增加 v1.20 reuse decision：

- `componentProductionOwnerReusePolicyVersionV1_20=flow-ipc-component-owner-reuse-policy-v1_20`
- `componentProductionOwnerReusePolicyV1_20=owner-first-then-focused-fallback-v1_20`
- `componentProductionOwnerReuseDecision=owner-reused` 或
  `focused-fallback-after-owner-owned-runtime-attempt`
- `componentProductionOwnerReuseDecisionReason=...`
- `componentProductionOwnerReuseDecisionTags=...`
- `componentProductionOwnerReuseDecisionPhase=before-component-imports`

验收要求：

- 如果 owner-owned runtime 满足，component resolver 不能再把同一 tag 的 focused import 成功
  当作必要成功条件。
- 如果 owner-owned runtime 不满足，component resolver 仍可 fallback，但 gate 必须证明已经先经过
  owner-first runtime attempt。

### 4. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_20-production-owner-owned-runtime`：只验证 v1.20 owner-first runtime
  reuse / fallback delta。
- `--require-flow-ipc-v1_20`：累计阶段完成 gate，表示 v1.19 baseline + v1.20 delta。

规则：

- 必须看到 v1.18 custom-scheme owner root/dependency graph 证据。
- 必须看到 v1.19 module import vs side-effect separation 证据。
- 必须看到 v1.20 owner-first phase evidence。
- 必须看到 component resolver reuse/fallback decision。
- 如果未满足 owner-owned runtime，failure layer 必须来自 v1.20 acceptable failures。
- 不允许以 focused component import success、root Blob fallback、v1.17 lazy dependency success、
  或 `productionBundleRuntimeOwnerModuleImported=true` 作为 v1.20 delta 通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_20.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.20 delta/cumulative gate。
- 保持 v1-v1.19 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 5. Documentation

更新：

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

文档必须明确：

- v1.20 是 production owner-owned runtime reuse attempt，不是 full production bundle parity。
- owner-first import resolve 不等于 side-effect success。
- focused fallback 只有在 owner-first attempt 失败后才可作为 fallback 路径记录。
- 如果 v1.20 仍走 acceptable blocker path，必须给出 failure layer、source artifact、runtime phase、
  browser/native error 和下一步拆解方向。

## 验收命令

基础检查：

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

Server-side smoke：

```bash
bash bin/flow-client-resource-smoke
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
```

Trace fixture：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19 docs/traces/flow-ipc-v1_19.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20-production-owner-owned-runtime docs/traces/flow-ipc-v1_20.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20 docs/traces/flow-ipc-v1_20.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20 .build/vaadin-desktop-host-swift/host.out.log
```

Framework validation：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.20 可以用两种方式完成：

1. Preferred：owner-first custom-scheme production runtime path 产生可观察 reusable side
   effects，component resolver 记录 `owner-reused`，并且 TextField / Checkbox / Select 的真实
   event -> Java listener -> UIDL patch -> visible DOM 路径继续通过。
2. Acceptable：owner-first attempt 真实运行，但 owner-owned runtime 仍不满足；failure layer
   必须从 v1.19 的 side-effect decision 推进到 v1.20 owner-owned runtime contract 层，并记录
   `focused-fallback-after-owner-owned-runtime-attempt`，同时组件行为不退化。
