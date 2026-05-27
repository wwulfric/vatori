# Goal 1.21: Unified Frontend Resource Resolver Contract

状态：已完成。

完成摘要：v1.21 已在 Java manifest / resource response、Swift host `resourceStage` /
`resourceLoad` / `webview.dom` summary 和 `DesktopTraceInspector` gate 中落地统一
`frontendResolverContract`。当前 trace gate 证明同一 resolver contract 覆盖
flow-client classpath、component frontend、theme/static、generated/lazy、Flow lazy dependency、
production bundle / owner 和 dev-mode package graph，并同时分类 Blob staging、
`vatori-resource` custom-scheme/file-backed loading、verified-only resource、strict blocker 和
accepted fallback。v1.20 owner-owned runtime fallback 仍明确为 accepted blocker，不计为
production owner side-effect success。

`docs/goal.md` 是长期愿景和完成度账本。v1.20 已经证明 production owner-first
custom-scheme runtime attempt 会在 focused component imports 之前发生，并把当前 fallback
收敛到 `production-owner-owned-missing-real-artifact` /
`focused-fallback-after-owner-owned-runtime-attempt`。这让 production owner 路径不再只是
after-check 诊断，但 frontend/resource pipeline 仍由多条专用 resolver 拼在一起：
component frontend、generated/lazy、Flow lazy dependency、theme/static、production owner 和
dev-mode package graph 各自记录自己的 identity、cache、cleanup、scheme 和 integrity 语义。

**核心目标：把现有 component / generated-lazy / theme-static / flow-lazy-dependency /
production-owner / dev-mode package graph 的资源解析证据收束成一个统一
`frontendResolverContract`，让 trace gate 能证明同一套 resolver contract 覆盖所有当前
artifact families。**

## 为什么选择这个目标

继续直接冲 production owner side-effect success、真实 generated chunk file 或 dev-mode/Vite
execution 都有价值，但当前 trace 结构已经明显膨胀：每条资源路径都有独立字段，难以判断
pipeline 是“同一个框架能力”，还是几个烟测特例。要接近基本闭环，需要先让资源解析层变成
可产品化 contract：

- 每个 artifact family 都声明统一 resolver identity；
- body integrity、module identity、cache、cleanup、allowed scheme、no-localhost proof 可横向比较；
- Blob fallback、custom-scheme/file-backed path、verified-only resource 和 strict blocker 有统一分类；
- inspector 可以从统一 contract 判断 pipeline 覆盖，而不是为每个历史版本写一组孤立字段。

v1.21 因此优先做 resolver contract 收束。它不要求解决 production owner side-effect 或 generated
chunk file，但必须减少“各切片各说各话”的风险。

## 继承边界

以下能力在 v1.21 中视为基线，不能回退：

- v1.20 owner-first runtime reuse attempt 继续通过，且 current accepted blocker 仍必须在
  v1.20 owner-owned runtime contract 层。
- v1.18 production owner root/dependency graph 继续使用 `vatori-resource://flow-ipc/production-owner/...`
  file-backed custom-scheme URL；root Blob fallback 不能作为通过条件。
- v1.17 Flow lazy dependency 继续证明 host-owned custom-scheme/file-backed loader 可执行。
- v1.8 generated/lazy chunk + markdown helper staged execution、v1.11 real UIDL
  `Flow.loadOnDemand(chunkId)` trigger、v1.12 no-op chunk-file proof继续通过。
- BigDecimalField、TextField、Checkbox、Select、native input/checkbox、Button、file transfer、
  capability、push ordering、resync 和 framework app validation 不退化。
- Legacy TextField shim 只能作为旧路径回归保护，不能计入 v1.21 成功。

## 非目标

- 不要求本阶段完成 production owner side-effect success 或 `owner-reused`。
- 不要求生成真实 `./chunks/chunk-*.js` lazy chunk file。
- 不要求完成 dev-mode/Vite graph execution。
- 不扩大组件覆盖到 Grid/VirtualList、ComboBox、DatePicker、Dialog 或 Upload。
- 不把 localhost HTTP、`file://` 直读、unclassified Blob root fallback 或 focused component import
  success 当成 resolver contract 成功。

## 实现范围

### 1. Java Resource Contract Metadata

Java manifest / resource response 增加统一 frontend resolver contract metadata，至少包括：

- `frontendResolverContractVersion=flow-ipc-frontend-resolver-contract-v1_21`
- `frontendResolverContractScope=flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode`
- `frontendResolverIdentityPolicy=stable-source-path-plus-owner-namespace-v1_21`
- `frontendResolverIntegrityPolicy=sha256-required-for-bodied-resources-v1_21`
- `frontendResolverCachePolicy=window-lifetime-content-addressed-v1_21`
- `frontendResolverCleanupPolicy=host-owned-window-lifetime-v1_21`
- `frontendResolverAllowedSchemes=blob,vatori-resource`
- `frontendResolverNoLocalhost=true`
- `frontendResolverFamiliesCovered=flow-client-classpath,vaadin-component-classpath-frontend,npm-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-bundle,dev-mode-bundle`
- `frontendResolverAcceptableGaps=generated-lazy-chunk-file-not-generated,production-owner-owned-missing-real-artifact,dev-mode-vite-execution-unmapped`

验收要求：

- `flow-client-resource-smoke` 断言 bootstrap manifest 和代表性 resource response 带 v1.21
  contract metadata。
- v1.4-v1.20 metadata 继续保留，历史 gates 不退化。

### 2. Host-Side Resolver Summary

Swift host 聚合每次 `resourceRequest` / `resourceResponse` / `resourceStage` / `resourceLoad`
证据，形成一个统一 DOM summary：

- `frontendResolverContractObserved=true`
- `frontendResolverContractVersion=flow-ipc-frontend-resolver-contract-v1_21`
- `frontendResolverFamiliesObserved=...`
- `frontendResolverFamiliesWithBodyIntegrity=...`
- `frontendResolverFamiliesWithStableIdentity=...`
- `frontendResolverSchemesObserved=blob,vatori-resource`
- `frontendResolverCustomSchemeFamilies=flow-lazy-dependency,production-bundle`
- `frontendResolverBlobFamilies=component-frontend,generated-lazy,...`
- `frontendResolverVerifiedOnlyFamilies=flow-client-classpath,dev-mode-bundle`
- `frontendResolverStrictBlockerFamilies=generated-lazy,production-bundle,dev-mode-bundle`
- `frontendResolverNoLocalhost=true`
- `frontendResolverContractComplete=true` 或 explicit failure reason

验收要求：

- 至少一个 Blob-backed executed family 和至少一个 `vatori-resource` file-backed executed/imported
  family 被同一个 contract summary 覆盖。
- Production owner v1.20 fallback 只能作为 `production-owner-owned-missing-real-artifact`
  这类 accepted gap 记录，不能被包装成 success。
- Verified-only resources 必须明确归类，不能看起来像执行成功。

### 3. Inspector Gate

新增两类 gate：

- `--require-flow-ipc-v1_21-frontend-resolver-contract`：只验证 v1.21 统一 resolver contract。
- `--require-flow-ipc-v1_21`：累计阶段完成 gate，表示 v1.20 baseline + v1.21 delta。

规则：

- 必须看到 v1.21 manifest / request / response / stage 或 DOM summary evidence。
- 必须覆盖 current artifact families：flow-client classpath、component frontend、theme/static、
  generated/lazy、Flow lazy dependency、production bundle / owner、dev-mode package graph。
- 必须看到 no-localhost proof、sha256/body integrity、stable identity/cache/cleanup policy、Blob 与
  `vatori-resource` 两类 scheme。
- 如果某 family 仍是 blocker，failure layer 必须来自 v1.21 accepted gaps 或历史 strict blocker。
- 不允许以 focused component import success、production owner module import resolve、
  unclassified fallback 或 compatibility mode 作为 v1.21 delta 通过条件。

交付：

- 新增 `docs/traces/flow-ipc-v1_21.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.21 delta/cumulative gate。
- 保持 v1-v1.20 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 4. Documentation

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

- v1.21 是统一 resolver contract，不是 production bundle/dev-mode/generated chunk parity。
- Contract 覆盖 verified-only、executed、strict-blocker 和 fallback 分类。
- v1.20 owner-owned runtime fallback 仍是 accepted blocker，不是 owner reuse success。
- 下一步可以基于统一 contract 选择真实 generated chunk file、production owner side-effect success
  或 dev-mode/Vite execution。

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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20 docs/traces/flow-ipc-v1_20.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21-frontend-resolver-contract docs/traces/flow-ipc-v1_21.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21 docs/traces/flow-ipc-v1_21.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21 .build/vaadin-desktop-host-swift/host.out.log
```

Framework validation：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.21 完成时必须满足：

1. 当前所有主要 artifact families 都能在 trace 中映射到统一 `frontendResolverContract`。
2. Contract summary 同时覆盖 Blob staging、host-owned custom scheme、verified-only resource、
   strict blocker 和 accepted fallback。
3. v1.20 production owner-owned runtime fallback 继续明确，不被包装成 success。
4. `flow-client-resource-smoke`、v1.21 trace gates、真实窗口 smoke、framework validation 和基础编译检查全部通过。

完成证据：

- `docs/traces/flow-ipc-v1_21.ndjson` 已新增为 v1.21 fixture。
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21-frontend-resolver-contract docs/traces/flow-ipc-v1_21.ndjson`
  通过。
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21 docs/traces/flow-ipc-v1_21.ndjson`
  通过。
- 真实窗口 smoke 使用 `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1` 和 auto-shutdown hooks 通过，并且
  `.build/vaadin-desktop-host-swift/host.out.log` 通过 `--require-flow-ipc-v1_21`。
- Framework validation app 使用 `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1` 和 auto-shutdown hooks
  通过，并且 host log 通过 `--require-flow-ipc-v1_3-framework`。
