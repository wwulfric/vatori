# Goal 1.23: Production Owner Positive Runtime Fixture Boundary

状态：计划中。

`docs/goal.md` 是长期愿景和完成度账本。v1.22 已经证明 generated
`Flow.loadOnDemand` chunk-file table 在真实 chunk 存在时，当前 no-localhost resolver、
parser、staging/import、callback 和 side-effect proof 可以闭环。剩余最靠近
Flow IPC frontend/resource pipeline 基本闭环的 blocker 是 production owner：
真实 Vaadin dev-bundle 的 stats-selected production `indexhtml` artifact 已经能通过
custom-scheme owner graph stage/import，但 owner-owned runtime side effect 仍未满足，
组件路径仍需要 focused fallback。

**核心目标：新增一个 repository-local、Vaadin production-owner 同形状的正向 owner runtime
fixture，走和 production owner 相同的 IPC resource response、host-owned custom scheme、
owner namespace、dependency rewrite、dynamic import 和 side-effect verification path；
证明当 owner runtime artifact 具备可复用 side effect contract 时，当前 resource pipeline
能完成 owner module import、side-effect proof 和 owner-owned reuse decision。同时继续明确
真实 app/build 的 production owner 仍停在 missing-real-artifact / focused fallback blocker。**

## 为什么选择这个目标

v1.18-v1.20 已经把 production owner 从 Blob staging 推进到 custom-scheme owner graph，
并且记录了 owner-first runtime attempt。但当前真实 Vaadin production artifact 仍没有暴露
一个可被本 runtime 直接复用的 owner-owned side-effect contract，trace 只能证明 fallback
是显式发生的。

v1.23 不把 fixture 包装成“真实 Vaadin production bundle 已成功”。它要补齐的是正向边界：

- owner root 和 owner dependency 都通过 `vatori-resource://flow-ipc/production-owner/...`
  加载。
- relative import rewrite、module identity、sha256 integrity、cache/cleanup 和 no-localhost
  语义都复用 v1.21 frontend resolver contract。
- WebView 可以观测 owner module import、expected tags/globals、side-effect satisfied、
  owner-owned runtime satisfied 和 reuse decision。

这样下一阶段判断会更清楚：如果真实 production bundle 继续失败，失败来自真实 Vaadin artifact
contract 或 build output，而不是当前 IPC/custom-scheme/resource-loader 基础设施没有正向执行能力。

## 继承边界

以下能力在 v1.23 中视为基线，不能回退：

- v1.22 generated lazy chunk-file positive execution fixture 继续通过。
- v1.21 unified frontend resolver contract 继续通过。
- v1.20 production owner-owned runtime fallback 继续保留真实 blocker 证据。
- v1.18 production owner custom-scheme root/dependency graph 继续通过。
- v1.17 host-owned resource scheme loader 继续通过。
- BigDecimalField、TextField、Checkbox、Select、framework validation、push ordering、
  resync、capability 和 file transfer 不退化。

## 非目标

- 不要求真实 Vaadin dev-bundle `indexhtml-*.js` production owner side effect 成功。
- 不把 fixture side effect 当成 production bundle parity。
- 不要求解决 dev-mode/Vite execution。
- 不要求当前 app/build 生成真实 generated lazy chunk file。
- 不扩大组件覆盖到 Grid/VirtualList、ComboBox、DatePicker、Dialog 或 Upload。
- 不使用 localhost HTTP、`file://` 直读或 compatibility mode 作为成功证据。

## 实现范围

### 1. Production Owner Fixture Resources

新增 classpath fixture：

- `META-INF/vatori/production-owner/owner-runtime-v1_23.js`
- `META-INF/vatori/production-owner/owner-dependency-v1_23.js`

fixture root 必须采用 production owner 同形状的 ES module：

```js
import { ownerDependencyV123 } from './owner-dependency-v1_23.js';

window.VaadinDesktopProductionOwnerV123 = {
  imported: true,
  dependency: ownerDependencyV123,
  executionVersion: 'flow-ipc-production-owner-positive-runtime-v1_23'
};
```

dependency 必须产生可验证 side effect，例如：

- `window.VaadinDesktopProductionOwnerDependencyV123.executed=true`
- `window.VaadinDesktopProductionOwnerV123.tags=['vatori-production-owner-fixture']`
- `window.VaadinDesktopProductionOwnerV123.globals=['window.VaadinDesktopProductionOwnerV123']`
- custom event 或 diagnostic marker，便于 WebView 验证

### 2. Java Resource Metadata

Java manifest / resource response 增加 v1.23 metadata：

- `productionBundleRuntimeOwnerPositiveFixtureVersion=flow-ipc-production-owner-positive-runtime-v1_23`
- `productionBundleRuntimeOwnerPositiveFixtureRootArtifact=...owner-runtime-v1_23.js`
- `productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact=...owner-dependency-v1_23.js`
- `productionBundleRuntimeOwnerPositiveFixtureExpectedTags=vatori-production-owner-fixture`
- `productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals=window.VaadinDesktopProductionOwnerV123`
- `productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy=owner-first-custom-scheme-positive-fixture-v1_23`
- `productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback=true`

验收要求：

- `flow-client-resource-smoke` 断言 root/dependency fixture response 都带 metadata。
- 真实 production owner v1.20 blocker metadata 继续保留，不被 v1.23 改写为 success。

### 3. Host Execution Path

Swift host / WebView runtime 增加受控 v1.23 owner fixture probe：

- 通过 IPC 请求 fixture root 和 dependency body。
- 使用 production owner custom-scheme namespace stage root/dependency。
- 将 root 内的 `./owner-dependency-v1_23.js` relative import 重写到 staged dependency URL。
- 在 focused component imports 之前 dynamic import fixture root。
- 验证 dependency side effect、root side effect、expected tags/globals。
- 记录 `resourceStage` 和 `webview.dom` 证据：
  - `productionBundleRuntimeOwnerPositiveFixtureObserved=true`
  - `productionBundleRuntimeOwnerPositiveFixtureStageObserved=true`
  - `productionBundleRuntimeOwnerPositiveFixtureImported=true`
  - `productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied=true`
  - `productionBundleRuntimeOwnerPositiveFixtureOwnerOwnedRuntimeSatisfied=true`
  - `componentProductionOwnerReuseDecision=owner-reused-positive-fixture`
  - root/dependency artifact、sha256、custom-scheme URL policy、no-localhost proof

验收要求：

- v1.23 fixture execution 不能改变真实 production owner v1.20 的
  `production-owner-owned-missing-real-artifact` / focused fallback 判定。
- 如果 fixture import 失败，trace 必须记录 browser/native error 和 failure layer。

### 4. Inspector Gate

新增两类 gate：

- `--require-flow-ipc-v1_23-production-owner-positive-fixture`：只验证 v1.23 正向 owner fixture。
- `--require-flow-ipc-v1_23`：累计阶段完成 gate，表示 v1.22 baseline + v1.23 delta。

规则：

- 必须看到 v1.23 manifest / resource response / resourceStage / DOM evidence。
- 必须看到 root 和 dependency 都通过 `vatori-resource` custom scheme 加载。
- 必须看到 owner module imported、side-effect satisfied、owner-owned runtime satisfied。
- 必须同时看到真实 production owner v1.20 blocker 仍然存在。

交付：

- 新增 `docs/traces/flow-ipc-v1_23.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.23 delta/cumulative gate。
- 保持 v1-v1.22 fixture gates 继续可运行。
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

- v1.23 是 production owner positive runtime fixture，不是当前 app/build production
  owner parity。
- 真实 Vaadin production owner blocker 仍保留。
- v1.23 的价值是证明 custom-scheme owner runtime path 在具备 owner contract 时可以闭环。
- 下一步仍需要让真实 production bundle 暴露可复用 owner contract，或选择 dev-mode/Vite
  execution / packaging-time staging 作为产品化路径。

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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22 docs/traces/flow-ipc-v1_22.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23-production-owner-positive-fixture docs/traces/flow-ipc-v1_23.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23 docs/traces/flow-ipc-v1_23.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23 .build/vaadin-desktop-host-swift/host.out.log
```

真实窗口 framework validation：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```
