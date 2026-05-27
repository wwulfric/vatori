# Goal 1.18: Production Owner Custom-Scheme Resource Graph

状态：已完成。

`docs/goal.md` 是长期愿景和完成度账本。v1.17 已把 Flow `LoadMode.LAZY`
dependency 的一条真实执行路径从 Blob-only staging 推进到 Swift host-owned
`vatori-resource` custom-scheme / file-backed loader。v1.18 承接这个边界，目标是把
production owner root/dependency graph 从显式 Blob fallback 推进到 host-owned resource
graph execution boundary。

**核心目标：让 production bundle runtime owner 使用 `vatori-resource://...` file-backed
custom-scheme URL stage/import 真实 stats-selected `indexhtml` root artifact 和已解析 owner
dependencies，并用 trace 证明 root/dependency request/response、relative import base、module
identity、owner-first ordering、component reuse decision 和最终 blocker 已经处在
custom-scheme owner graph 层。**

## 为什么选择这个目标

v1.17 证明了 `WKURLSchemeHandler` / file-backed loader 不是空壳：真实 Flow lazy dependency
已经通过 host-owned `vatori-resource://...` 执行。但 production owner 仍明确停在 Blob
fallback，这会让后续 production bundle import/side-effect work 继续受 Blob URL lifetime、
source rewrite、module identity 和 cleanup 语义影响。

v1.18 应该直接吃掉这个最大 fallback：production owner root 和已解析 dependencies 必须进入
host-owned resource graph。即使最终仍然停在 duplicate custom element、missing real artifact
或 Vaadin runtime ownership 层，blocker 也必须来自 custom-scheme owner graph，而不是 Blob
fallback。

## 继承边界

以下能力在 v1.18 中视为基线，不能回退：

- `dev-ipc` 继续启动 no-localhost Flow IPC runtime，核心 UI path 不通过 localhost HTTP。
- v1.17 Flow lazy dependency 继续通过 `vatori-resource://...` file-backed loader 执行。
- v1.16 production owner ordering / component reuse gate 继续通过。
- v1.15 owner dependency resolver closure 继续通过，不能回退到未分类
  `production-owner-relative-import-unresolved`。
- v1.13/v1.14 production owner contract/import attempt evidence 继续可审计。
- v1.7 TextField graph success、v1.8 generated/lazy execution、v1.10 ordinary lazy dependency、
  v1.11 real UIDL `Flow.loadOnDemand(chunkId)` trigger、v1.12 no-op chunk-file proof 继续通过。
- Button、BigDecimalField、TextField、Checkbox、Select、native input/checkbox、list update、
  desktop capability、file transfer、push ordering、resync、framework app validation 不退化。
- Legacy TextField shim 仍只能作为旧路径回归保护，不能计入 v1.18 成功。

## 非目标

- 不要求本阶段完成完整 production bundle import/side-effect success。
- 不要求本阶段完成 dev-mode/Vite execution。
- 不要求本阶段生成真实 `./chunks/chunk-*.js` lazy chunk file。
- 不要求一次性迁移所有 component/frontend artifacts 到 custom scheme。
- 不把 localhost HTTP、`file://` 直读或 compatibility host 作为 v1.18 成功证据。
- 不为了迁移 production owner graph 牺牲现有 component event chain。

## 实现范围

### 1. Production Owner Resource Loader Contract

目标：

- Java manifest / resource response 为 production owner root 和 owner dependencies 增加
  v1.18 metadata：
  - `productionBundleRuntimeOwnerResourceLoaderVersion=flow-ipc-production-owner-resource-loader-v1_18`
  - `productionBundleRuntimeOwnerResourceLoaderMode=custom-scheme-file-backed`
  - `productionBundleRuntimeOwnerResourceScheme=vatori-resource`
  - `productionBundleRuntimeOwnerResourceGraphMode=owner-root-and-resolved-dependencies`
  - `productionBundleRuntimeOwnerResourceBasePolicy=stable-owner-resource-path-base-v1_18`
  - `productionBundleRuntimeOwnerBlobFallbackAllowed=false` for root artifact
  - `productionBundleRuntimeOwnerDependencyBlobFallbackPolicy=explicit-classified-only`
- 保留 v1.13-v1.17 metadata，不能破坏历史 gates。

验收要求：

- `flow-client-resource-smoke` 断言 v1.18 production owner resource loader metadata。
- `bootstrapResponse` / `resourceResponse` 中能区分 component focused graph、Flow lazy
  dependency loader 和 production owner custom-scheme graph。

### 2. Host-Owned Owner Graph Staging

目标：

- Swift host 为 production owner root artifact 和已解析 owner dependencies 注册
  `vatori-resource://flow-ipc/production-owner/...` URL。
- Root artifact 和至少一个真实 resolved dependency 必须有 file-backed stage evidence：
  - source path；
  - owner namespace；
  - MIME；
  - byte count / sha256；
  - redacted file path；
  - resource base URL；
  - relative import base；
  - cache hit/miss；
  - cleanup policy；
  - no-localhost proof。
- 优先让 relative import 使用 scheme URL 的原生 relative base；如果仍需要 source rewrite，
  trace 必须说明 rewrite 发生在 production owner custom-scheme layer，而不是 Blob layer。

验收要求：

- Trace 中出现 `resourceLoad` request/response for production owner root。
- Trace 中出现 `resourceLoad` request/response for at least one owner dependency。
- Production owner root 不允许再以 `stagingMode=blob-fallback` 作为 v1.18 通过条件。

### 3. Owner Import Attempt Over Custom Scheme

目标：

- `before-component-imports` owner-first preflight 通过 custom-scheme URL 发起 root import。
- 如果 root import 继续失败，failure layer 必须是下列之一：
  - `production-owner-custom-scheme-missing-real-artifact`
  - `production-owner-custom-scheme-duplicate-custom-element`
  - `production-owner-custom-scheme-runtime-side-effect-blocked`
  - 更精确、可解释的 downstream browser/runtime layer
- `production-owner-relative-import-unresolved`、`production-owner-runtime-contract-missing`、
  `production-owner-missing-real-artifact` 只能作为历史字段出现，不能作为 v1.18 delta 通过条件。

验收要求：

- `resourceStage` / `webview.dom` 记录：
  - `productionBundleRuntimeOwnerResourceLoaderVersion=flow-ipc-production-owner-resource-loader-v1_18`
  - owner root custom-scheme URL；
  - owner dependency custom-scheme URL count；
  - import attempted over custom scheme；
  - `window.Vaadin` before/after；
  - custom element registry before/after；
  - component reuse decision；
  - final blocker layer。

### 4. Inspector Gate Semantics

新增两类 gate：

- `--require-flow-ipc-v1_18-production-owner-resource-scheme`：只验证 v1.18 production owner
  custom-scheme graph delta。
- `--require-flow-ipc-v1_18`：累计阶段完成 gate，表示 v1.17 baseline + v1.18 delta。

规则：

- v1.18 delta 必须看到 production owner root 的 custom-scheme request/response/import attempt。
- v1.18 delta 必须看到至少一个 production owner dependency 的 custom-scheme request/response。
- v1.18 delta 不能只复用 v1.17 Flow lazy dependency 的 `vatori-resource` 证据。
- v1.18 delta 不能以 production owner root Blob fallback 通过。
- 如果 dependency 仍有 fallback 或 missing artifact，必须记录 path/specifier 和 failure layer。

交付：

- 新增 `docs/traces/flow-ipc-v1_18.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.18 delta/cumulative gate。
- 保持 v1-v1.17 fixture gates 继续可运行。
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

- v1.18 是 production owner custom-scheme graph boundary，不是 full production bundle parity。
- v1.17 的 Flow lazy dependency custom-scheme success 不能被复用成 v1.18 owner graph success。
- production owner root Blob fallback 不能再作为 v1.18 通过条件。
- 如果最终仍是 blocker，blocker 必须来自 custom-scheme owner graph 层。

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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17 docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18-production-owner-resource-scheme docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18 docs/traces/flow-ipc-v1_18.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18 .build/vaadin-desktop-host-swift/host.out.log

VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.18 可以用两种方式完成：

1. Preferred：production owner root 和所有已解析 owner dependencies 都通过
   `vatori-resource://...` file-backed custom-scheme URL stage/import，relative imports 不再需要
   Blob source rewrite，并且 owner import 的最终 blocker 推进到 runtime side-effect 或
   component reuse 层。
2. Acceptable：production owner root 和至少一个真实 owner dependency 通过 custom-scheme
   request/response/import attempt；剩余 dependency 缺失或 fallback 被精确分类。Root artifact
   不能再以 Blob fallback 作为通过条件。

无论采用哪种方式，都必须新增 v1.18 trace fixture、strict inspector gate、文档更新和真实窗口
验证结果。
