# Goal 1.38: Current App Owner Component Reuse Matrix

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.37 已把 current app production owner 的
side-effect proof 接入 TextField 组件复用：当 owner 已注册 `vaadin-text-field`，host 会跳过
focused TextField dev-bundle import。但这仍是单点成功，不代表更早执行的
BigDecimalField、Checkbox 和 Select 也已经能复用 current app owner。

v1.38 的目标是把 v1.37 的单点复用推进为可审计的组件矩阵：当前 smoke app 覆盖的四个真实
Vaadin component tag 都必须在 trace 中被观察、分类，并明确是 owner reuse 还是 focused fallback。
这一步不要求强行改变执行顺序；它要求不再用 TextField 的成功暗示其他组件也成功。

## 核心目标

新增 `flow-ipc-current-app-owner-component-reuse-matrix-v1_38` contract，覆盖：

- Matrix metadata：Java manifest / resource response 声明 matrix version、source、policy、
  expected tags、decision、failure layer 和 next step。
- Per-component classification：Swift host 在每个 focused component import 前记录目标 tag、
  focused import path、是否复用 current app owner、是否 fallback，以及 fallback reason。
- Current app ordering evidence：trace 必须证明 BigDecimalField、Checkbox、Select 的 fallback
  是因为它们早于 current app owner reuse readiness，而不是因为缺少 IPC resource resolver。
- TextField reuse retained：`META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` 仍必须记录
  current app owner reuse、skipped focused import，并保持真实 Flow `TextField` listener -> UIDL patch
  -> visible DOM 更新。
- Blocker narrowing：matrix decision 必须明确当前状态是“TextField 复用 + 早期组件 focused fallback”，
  下一步才是调整 owner import order / supported Vaadin build SPI / broader component parity。

## Acceptable outcomes

- Preferred：四个 tag 都被 matrix 观察；TextField 复用；BigDecimalField、Checkbox、Select 被明确
  classified 为 current-app-owner-not-ready fallback；trace gate 要求这些字段同时存在。
- Acceptable：某个早期组件也能复用 owner；matrix 仍需记录它的 reused tag 和 skipped focused import，
  并且不能丢失其他组件的分类。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 v1.37 TextField 单点复用当作四组件泛化成功。
- 不通过删除 BigDecimalField、Checkbox 或 Select 覆盖来让 gate 变绿。
- 不引入 localhost/Vite dev server 作为成功证据。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38-current-app-owner-component-reuse-matrix .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_38.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38-current-app-owner-component-reuse-matrix docs/traces/flow-ipc-v1_38.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38 docs/traces/flow-ipc-v1_38.ndjson
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
feat: add current app owner reuse matrix
```

## 完成记录

v1.38 已把 v1.37 的 TextField 单点 current app owner reuse 扩展成四组件可审计矩阵：

- Java manifest / resource response 声明
  `flow-ipc-current-app-owner-component-reuse-matrix-v1_38` metadata、expected tags、
  policy、failure layer 和 next step。
- Swift host 在 covered component focused import 前聚合 matrix diagnostics，观察
  `vaadin-big-decimal-field`、`vaadin-checkbox`、`vaadin-select` 和
  `vaadin-text-field` 四个 tag。
- Trace 记录前三个早期组件走 focused fallback，ordering blocker 为
  `current-app-owner-not-ready-before-focused-import`；TextField 继续复用 current app owner，
  并跳过 `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` focused import。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_38-current-app-owner-component-reuse-matrix` 和
  `--require-flow-ipc-v1_38` gates；v1.38 request gate 验证 covered component resource lookup，
  matrix contract 本身由 manifest/response/stage/DOM 证明。
- 固定 trace 已保存到 `docs/traces/flow-ipc-v1_38.ndjson`。

剩余 blocker 没有消失：owner import order / supported Vaadin build SPI、真实 app/build
generated chunk-file parity、dev-mode/Vite parity 和 broader component/resource parity 仍未完成。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37-current-app-production-owner-component-reuse .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38-current-app-owner-component-reuse-matrix .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38-current-app-owner-component-reuse-matrix docs/traces/flow-ipc-v1_38.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38 docs/traces/flow-ipc-v1_38.ndjson
```
