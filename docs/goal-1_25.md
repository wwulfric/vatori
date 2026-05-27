# Goal 1.25: Packaging-Time Staged Frontend Baseline

状态：待实施

`docs/goal.md` 是长期愿景和完成度账本。v1.24 已完成
`flow-ipc-frontend-pipeline-closure-v1_24`，证明 no-localhost frontend/resource
pipeline 的代表性基础设施已经闭合，同时明确真实 app/build 的 generated chunk、
production owner 和 dev-mode/Vite parity 仍未完成。

v1.25 的目标是从 v1.24 的决策层继续向产品路径推进：选择
**packaging-time staged frontend** 作为 v0/v1 可落地 baseline，并把 runtime 中已经能执行的
frontend families 从“临时请求后 Blob staging 为主”推进到“host-owned inventory +
custom-scheme/file-backed staging 为主”的产品化形态。它不是完整打包系统，也不要求
Vaadin production/dev-mode parity 在本阶段完成。

## 核心目标

新增 `flow-ipc-packaged-frontend-baseline-v1_25` contract，覆盖：

- host-owned frontend inventory：为 flow-client、component-frontend、theme/static、
  generated-lazy、flow-lazy-dependency、production-owner 和 dev-mode family 生成统一清单。
- packaging-time product path decision：
  `packaging-time-staged-frontend-baseline`，并保留受支持 Vaadin build SPI 作为后续评估项。
- custom-scheme/file-backed staging baseline：让代表性可执行 frontend artifacts 优先通过
  `vatori-resource://` host-owned resource loader 执行；Blob 只作为明确标记的 diagnostic fallback。
- inventory integrity：每个 staged/verified artifact 必须记录 source path、family、
  owner namespace、sha256、byte count、execution policy、staging mode、cleanup policy。
- blocker preservation：真实 generated chunk file、production owner owned runtime、
  dev-mode/Vite execution 仍按真实 app/build blocker 记录，不能被 v1.22/v1.23 fixture 覆盖。

v1.25 的完成条件是：trace inspector 可以证明 packaging-time staged frontend baseline
已经有可审计清单、host-owned staging evidence、custom-scheme execution evidence 和
real-build blocker 分类。它必须比 v1.24 更接近产品化运行路径，而不只是再次汇总历史证据。

## 不做什么

- 不实现完整 jpackage / installer / updater。
- 不宣布真实 Vaadin production bundle 或 dev-mode/Vite parity 已完成。
- 不把 v1.22 generated chunk fixture 或 v1.23 production owner fixture 当成真实 app/build 成功。
- 不扩大到 Grid/ComboBox/DatePicker/Dialog/Upload。
- 不删除 compatibility mode；但不能用 localhost compatibility mode 作为 v1.25 成功证据。

## 实现要求

### Java runtime metadata

`FlowClientResources` 的 manifest 和 resource response 要新增 v1.25 metadata：

- `packagedFrontendBaselineVersion=flow-ipc-packaged-frontend-baseline-v1_25`
- `packagedFrontendProductPath=packaging-time-staged-frontend-baseline`
- `packagedFrontendInventoryScope=flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode`
- `packagedFrontendPreferredLoader=custom-scheme-file-backed`
- `packagedFrontendAllowedFallback=diagnostic-blob-only`
- `packagedFrontendIntegrity=sha256-and-byte-count`
- `packagedFrontendCleanupPolicy=window-lifetime-cache-cleanup`
- `packagedFrontendRealBuildStatus=product-parity-pending`
- `packagedFrontendRemainingBlockers=real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped`

如果新增 Java-side inventory helper，必须保持它是 framework/runtime 代码，而不是
smoke-app 专用逻辑。

### Swift host / WebView diagnostics

Swift host / WebView 要把 v1.25 metadata 和 inventory evidence 传播到：

- `resourceRequest`
- `resourceResponse`
- `resourceStage`
- `resourceLoad`
- `webview.dom`

DOM summary 必须能证明：

- v1.25 inventory 被观察到，覆盖的 family 与 manifest 一致。
- 代表性 executable artifacts 通过 `vatori-resource://` custom-scheme/file-backed
  loader 执行或被明确分类为 diagnostic Blob fallback。
- 每个 host-owned staged artifact 有 source path、family、owner namespace、sha256、
  byte count、cleanup policy 和 no-localhost proof。
- v1.24 closure decision 仍保留，且真实 app/build blockers 仍被明确记录。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25-packaged-frontend-baseline docs/traces/flow-ipc-v1_25.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25 docs/traces/flow-ipc-v1_25.ndjson
```

`--require-flow-ipc-v1_25-packaged-frontend-baseline` 只验证 v1.25 delta；
`--require-flow-ipc-v1_25` 继承 v1.24 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25-packaged-frontend-baseline .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_25.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25-packaged-frontend-baseline docs/traces/flow-ipc-v1_25.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25 docs/traces/flow-ipc-v1_25.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 docs/traces/flow-ipc-v1_24.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/flow-transport-adapter.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/ipc-protocol-v1.md`
- `docs/native-host.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: add packaged frontend baseline gate
```
