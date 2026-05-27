# Goal 1.19: Production Owner Side-Effect and Reuse Decision

状态：已完成。

`docs/goal.md` 是长期愿景和完成度账本。v1.18 已经把 production owner root 和
resolved dependency graph 从 Blob fallback 推进到 `vatori-resource://...`
file-backed custom-scheme stage/import boundary。v1.19 承接这个边界，目标不是再证明
custom-scheme handler 可用，而是让 owner import 之后的 runtime side effect 和 component
reuse decision 进入可审计闭环。

**核心目标：在 production owner custom-scheme import 之后，明确记录 owner module import
是否完成、side effect 是否满足、期望 custom elements / Vaadin globals 是否出现、focused
component resolver 是否能复用 owner side effects；如果不能复用，blocker 必须收敛到具体
v1.19 side-effect / registry / Vaadin runtime contract 层，并且不能退回 Blob fallback。**

## 为什么选择这个目标

v1.18 的最大价值是把 production owner root/dependency graph 放到了 host-owned
custom-scheme URL 上，消除了 root Blob fallback 作为成功条件。但 trace 里仍然存在一个
语义混淆：dynamic import 可能已经 resolve，而真实可复用 side effect 仍不可观察。后续如果
继续只看 `productionBundleRuntimeOwnerImported=true`，就容易把“模块 import resolve”误判成
“production owner runtime 可复用”。

v1.19 要把这两个概念拆开：

- module import result：custom-scheme owner root 是否完成 dynamic import。
- side-effect result：真实 Vaadin globals、`Flow.loadOnDemand`、`vaadin-text-field`、
  `vaadin-checkbox`、`vaadin-select` 是否由 owner path 提供可复用 side effects。
- component reuse result：focused component resolver 是否复用 owner-owned tags，或为什么仍需
  focused fallback。

## 继承边界

以下能力在 v1.19 中视为基线，不能回退：

- v1.18 production owner root 和至少一个 resolved dependency 继续通过
  `vatori-resource://flow-ipc/production-owner/...` file-backed custom-scheme URL stage/import。
- production owner root Blob fallback 仍然不能作为通过条件。
- v1.17 Flow lazy dependency custom-scheme success 不能被复用成 owner graph 成功。
- v1.16 owner-first ordering / after-component fallback evidence 继续可审计。
- v1.15 dependency resolver closure、v1.14 import attempt、v1.13 owner contract 继续保留。
- v1.7 TextField graph success、v1.8 generated/lazy execution、v1.10 ordinary lazy dependency、
  v1.11 real UIDL `Flow.loadOnDemand(chunkId)` trigger、v1.12 no-op chunk-file proof 继续通过。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、list update、
  desktop capability、file transfer、push ordering、resync、framework app validation 不退化。
- Legacy TextField shim 仍只能作为旧路径回归保护，不能计入 v1.19 成功。

## 非目标

- 不要求本阶段完成完整 production bundle / dev-mode / Vite parity。
- 不要求本阶段生成真实 `./chunks/chunk-*.js` lazy chunk file。
- 不要求本阶段移除 focused component fallback；如果 owner side effect 不满足，focused fallback
  可以保留，但必须被明确判定和记录。
- 不把 `productionBundleRuntimeOwnerImported=true` 单独视为 production owner runtime 成功。
- 不把 localhost HTTP、`file://` 直读、Blob root fallback 或 compatibility host 作为 v1.19 成功证据。

## 实现范围

### 1. Side-Effect Contract Metadata

Java manifest / resource response 增加 v1.19 metadata：

- `productionBundleRuntimeOwnerSideEffectVersion=flow-ipc-production-owner-side-effect-v1_19`
- `productionBundleRuntimeOwnerSideEffectPolicy=custom-scheme-owner-import-side-effect-contract-v1_19`
- `productionBundleRuntimeOwnerSideEffectExpectedTags=vaadin-text-field,vaadin-checkbox,vaadin-select`
- `productionBundleRuntimeOwnerSideEffectExpectedGlobals=window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand`
- `productionBundleRuntimeOwnerSideEffectAcceptableFailures=production-owner-custom-scheme-runtime-side-effect-blocked,production-owner-custom-scheme-duplicate-custom-element,production-owner-custom-scheme-browser-import-failed,production-owner-custom-scheme-missing-real-artifact,production-owner-custom-scheme-unsupported-vaadin-contract`
- `productionBundleRuntimeOwnerSideEffectNoBlobFallback=true`
- `componentProductionOwnerReuseDecisionVersion=flow-ipc-component-owner-reuse-decision-v1_19`

验收要求：

- `flow-client-resource-smoke` 断言 manifest、production runtime response、至少一个 owner dependency
  带 v1.19 side-effect metadata。
- v1.13-v1.18 metadata 继续保留，历史 gates 不退化。

### 2. Host Side-Effect Diagnostics

Swift host 在 owner custom-scheme import 前后记录：

- module import 是否 attempted / resolved / rejected；
- side-effect registry before/after；
- expected tags、observed tags、missing tags；
- expected globals、global before/after；
- side-effect satisfied boolean；
- side-effect failure layer；
- no-Blob-fallback proof；
- owner root/dependency custom-scheme evidence 继续保留。

如果 import resolve 但 side effect 不满足，应记录：

- `productionBundleRuntimeOwnerModuleImported=true`
- `productionBundleRuntimeOwnerSideEffectSatisfied=false`
- `productionBundleRuntimeOwnerSideEffectFailureLayer=production-owner-custom-scheme-runtime-side-effect-blocked`

### 3. Component Reuse Decision

focused component resolver 记录 v1.19 reuse decision：

- `componentProductionOwnerReuseDecisionVersion=flow-ipc-component-owner-reuse-decision-v1_19`
- `componentProductionOwnerReuseDecisionSource=production-owner-custom-scheme-side-effect-v1_19`
- `componentProductionOwnerReuseDecision=owner-reused` 或
  `focused-fallback-after-owner-side-effect-check`
- `componentProductionOwnerReuseDecisionReason=...`
- `componentProductionOwnerReuseDecisionTags=...`

验收要求：

- 如果 side effect 满足，组件复用必须有 owner-owned tag 证据。
- 如果 side effect 不满足，focused fallback 仍可执行，但必须明确说是因为 v1.19 side-effect check
  未满足，而不是因为没有运行 owner custom-scheme graph。

### 4. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_19-production-owner-side-effect`：只验证 v1.19 production owner
  side-effect / component reuse decision delta。
- `--require-flow-ipc-v1_19`：累计阶段完成 gate，表示 v1.18 baseline + v1.19 delta。

规则：

- 必须看到 v1.18 custom-scheme owner root/dependency graph 证据。
- 必须看到 v1.19 side-effect metadata 和 runtime diagnostics。
- 必须区分 module import result 与 side-effect satisfied result。
- 必须看到 component reuse decision。
- 如果未满足 side effect，failure layer 必须来自 v1.19 acceptable failures。
- 不允许以 root Blob fallback、v1.17 lazy dependency custom-scheme success 或单纯
  `productionBundleRuntimeOwnerImported=true` 作为 v1.19 delta 通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_19.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.19 delta/cumulative gate。
- 保持 v1-v1.18 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 5. Documentation

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

文档必须明确：

- v1.19 是 production owner side-effect / component reuse decision boundary，不是 full
  production bundle parity。
- module import resolve 不等于 owner side effect success。
- focused fallback 可以保留，但不能被包装成 owner reuse。
- 下一步仍可能是 generated chunk-file、production owner supported runtime contract 或 dev-mode/Vite
  execution。

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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18 docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19-production-owner-side-effect docs/traces/flow-ipc-v1_19.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19 docs/traces/flow-ipc-v1_19.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.19 可以用两种方式完成：

1. Preferred：production owner custom-scheme import 产生可观察 side effects，expected tags/globals
   满足，focused component resolver 明确复用 owner-owned tags，记录 `owner-reused`。
2. Acceptable：owner custom-scheme import 真实运行且 module import result 与 side-effect result 被明确
   拆开；side effect 未满足时，blocker 收敛到 v1.19 side-effect / registry / unsupported Vaadin
   contract 层，并记录 `focused-fallback-after-owner-side-effect-check`，root artifact 不回退 Blob。

本阶段采用 acceptable path 完成：trace 记录了 production owner custom-scheme import 与
v1.19 side-effect diagnostics，明确区分 `productionBundleRuntimeOwnerModuleImported` 和
`productionBundleRuntimeOwnerSideEffectSatisfied`；当前 component resolver 仍走
`focused-fallback-after-owner-side-effect-check`，不能计入 production owner reuse success。
