# Goal 1.43: Generated Lazy Chunk SPI Decision Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.42 已把 current app production
`generated-flow-imports-*.js` support artifact 提升为明确 runtime contract：四个 covered
components 由 generated imports owner 作为 resolver handoff 输入稳定跳过 focused imports，且
trace 同步记录 lazy chunk SPI bridge metadata。

v1.43 的目标是把 v1.42 中的 `pending-supported-vaadin-lazy-chunk-spi` placeholder 推进为可审计
决策：要么接入一个受支持的 Vaadin build/runtime SPI，把 current app generated lazy chunk 作为产品化
runtime 输入；要么明确证明当前 Vaadin 版本没有可直接依赖的 SPI，并把 product path 收敛到
packaging-time `build-frontend` output staging contract，而不是继续把 repository fixture 或 historical
no-op table 当作闭环。

## 核心目标

新增 `flow-ipc-generated-lazy-spi-decision-v1_43` contract，覆盖：

- Vaadin source/SPI audit：trace 记录已审计的 Vaadin source surfaces，例如
  `AbstractUpdateImports`、`FrontendDependencies`、`ChunkInfo`、build-frontend generated output、
  production `stats.json` / `prod.bundle`，并说明是否存在 supported lazy chunk SPI。
- Current app chunk bridge hardening：把 v1.42 lazy chunk bridge 的 table shape、chunk id、chunk
  artifact、build output source、sha256/byte count 和 no-localhost/no-fixture proof 变成明确
  contract 字段。
- Runtime path decision：如果存在可接受 SPI，resolver 必须声明使用该 SPI 的 owner/path；如果不存在，
  必须声明 `generatedLazySpiDecision=packaging-time-build-frontend-staging` 或更精确的 failure layer，
  并给出下一步 product path。
- Anti-regression：v1.42 generated imports owner runtime contract 继续通过；四个 covered component
  focused imports 继续由 owner contract 跳过，visible BigDecimalField/Checkbox/TextField/Select event
  path 继续通过。

## Acceptable outcomes

- Preferred：通过受支持 Vaadin build/runtime SPI 取得 current app generated imports/chunk registry，
  并把 Flow-client-triggered `Flow.loadOnDemand(chunkId)` path 连接到该 SPI metadata，trace 记录
  `generatedLazySpiAvailable=true`、`generatedLazySpiRuntimePathSelected=true` 和 real current app chunk
  artifact import / callback / side-effect proof。
- Acceptable：源码和 build output audit 证明当前 Vaadin 版本没有可直接调用的 supported SPI；trace
  记录 audited source surfaces、current app generated chunk available/executed evidence、SPI unavailable
  reason、selected packaging-time staging product path 和 precise next resolution step。该结果必须比
  `pending-supported-vaadin-lazy-chunk-spi` 更具体，不能只重复 v1.42 placeholder。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 v1.22 generated chunk fixture 当作 current app SPI success。
- 不把 v1.12 dev-bundle no-op table 当作当前 app build output 状态。
- 不引入 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不一次性解决 dev-mode/Vite parity、Grid/ComboBox/Dialog/Upload broad component parity、Windows/Linux host、
  installer packaging 或 durable reconnect。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43-generated-lazy-spi-decision .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_43.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43-generated-lazy-spi-decision docs/traces/flow-ipc-v1_43.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43 docs/traces/flow-ipc-v1_43.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: add generated lazy SPI decision contract
```

## 完成记录

- Java manifest/resource response 新增 `flow-ipc-generated-lazy-spi-decision-v1_43`
  metadata：记录已审计的 Vaadin source/build surfaces、`ChunkInfo` 内部 API 状态、
  current app generated imports/chunk artifact、sha256/byte count、production support artifact、
  stats / `prod.bundle`、SPI unavailable reason、selected product path 和下一步。
- Swift host 在 `resourceRequest` / `resourceResponse` / `resourceStage` / `webview.dom`
  中传播 v1.43 contract 字段，并执行一次明确标注的
  `generatedLazySpiExecutionTrigger=host-decision-audit-probe-not-flow-client-spi`
  audit probe，证明 current app build-frontend chunk 可通过 no-localhost resolver stage/import，
  但不把它包装成 supported Flow-client SPI runtime path。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_43-generated-lazy-spi-decision` 和 `--require-flow-ipc-v1_43`
  gates；v1.43 cumulative gate 继续要求 v1.42 generated imports owner runtime contract 通过。
- 固定 trace 已保存到 `docs/traces/flow-ipc-v1_43.ndjson`。

实际完成结果：当前 Vaadin source/build audit 没有发现可直接依赖的 supported lazy chunk
runtime SPI；`ChunkInfo` 仍被记录为 internal / may-be-renamed-or-removed。v1.43 将 v1.42 的
`pending-supported-vaadin-lazy-chunk-spi` placeholder 收敛为
`generatedLazySpiDecision=packaging-time-build-frontend-staging` 和
`generatedLazySpiFailureLayer=supported-vaadin-lazy-chunk-spi-unavailable`。这证明下一步产品路径应是
framework-managed packaging-time `build-frontend` artifact inventory/staging，或推动上游/受支持
Vaadin SPI；它仍不是完整 frontend/resource pipeline 闭环。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43-generated-lazy-spi-decision .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43 .build/vaadin-desktop-host-swift/host.out.log
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_43.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43-generated-lazy-spi-decision docs/traces/flow-ipc-v1_43.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43 docs/traces/flow-ipc-v1_43.ndjson
```
