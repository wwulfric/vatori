# Goal 1.44: Packaging-Time Frontend Inventory Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.43 已经把 lazy chunk SPI placeholder 收敛为
明确决策：当前 Vaadin 版本没有可直接依赖的 supported lazy chunk runtime SPI，产品路径应转向
packaging-time `build-frontend` output staging。

v1.44 的目标是把这个决策推进为 framework-managed packaging-time frontend inventory contract：
CLI/runtime/trace 必须能够说明当前 app 的 `build-frontend` 产物如何被选择、校验、stage、cache、
cleanup，并如何交给 generated imports owner runtime contract。它仍不是完整 installer packaging、
dev-mode/Vite parity 或 broad component/resource parity。

## 核心目标

新增 `flow-ipc-packaging-time-frontend-inventory-v1_44` contract，覆盖：

- CLI/build preflight handoff：trace 记录 `build-frontend` goal、production mode、plugin/build status、
  output usability、output roots 和 framework-managed inventory mode。
- Inventory selection：trace 记录 selected generated imports、generated chunk、production support artifact、
  production entry、stats、`prod.bundle` 和 production build graph artifacts。
- Integrity and identity：selected artifact 必须带 sha256 / byte count 证据，并沿用 stable source-path /
  owner namespace identity。
- Loader/cache/cleanup：trace 记录 allowed loader scheme、custom-scheme primary、diagnostic Blob fallback、
  window-lifetime cache 和 close-time staged file cleanup policy。
- Runtime handoff：inventory contract 必须对齐 v1.42 generated imports owner runtime contract 和 v1.43
  SPI decision，且继续验证 covered component focused imports 被 owner contract 跳过。

## 不做什么

- 不声明完整 Flow IPC frontend/resource pipeline 闭环。
- 不把 host audit probe 当作 Flow-client-owned lazy chunk SPI success。
- 不引入 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不一次性解决 dev-mode/Vite、Grid/ComboBox/Dialog/Upload、installer packaging、Windows/Linux host 或 durable reconnect。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44-packaging-frontend-inventory .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_44.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44-packaging-frontend-inventory docs/traces/flow-ipc-v1_44.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44 docs/traces/flow-ipc-v1_44.ndjson
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
feat: add packaging frontend inventory contract
```

## 完成记录

- `bin/vaadin-desktop` 在 Flow IPC frontend build preflight 中转发
  `VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_44=1`，让 runtime trace 能区分显式
  packaging-time inventory contract。
- Java manifest / resource response 新增
  `flow-ipc-packaging-time-frontend-inventory-v1_44` metadata，记录 `build-frontend` goal/status、
  output roots、selected source generated imports/chunk、production support artifact、production
  entry/build graph、stats、`prod.bundle`、sha256/byte count integrity、loader scheme、cache/cleanup
  和 v1.42 runtime handoff policy。
- Swift host 在 `resourceStage` 和 `webview.dom` 中传播 v1.44 inventory diagnostics，证明 selected
  production generated imports support artifact 通过 no-localhost custom-scheme path stage/import，
  并且 runtime handoff 已由 generated imports owner runtime contract 满足。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_44-packaging-frontend-inventory` 和 `--require-flow-ipc-v1_44` gates。
  v1.44 cumulative gate 以 v1.42 runtime contract + v1.44 productized inventory 为准，不机械重放
  v1.43 host decision audit probe。
- 固定 trace 已保存到 `docs/traces/flow-ipc-v1_44.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44-packaging-frontend-inventory .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44-packaging-frontend-inventory docs/traces/flow-ipc-v1_44.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44 docs/traces/flow-ipc-v1_44.ndjson
```

v1.44 不声明完整 Flow IPC frontend/resource pipeline 闭环。剩余 blocker 仍包括
dev-mode/Vite parity、更广 component/resource parity、installer packaging 和 durable reconnect。
