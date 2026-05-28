# Goal 1.39: Current App Owner Early Import Ordering

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.38 已把 covered component matrix 讲清楚：
`vaadin-text-field` 能复用 current app production owner，但 `vaadin-big-decimal-field`、
`vaadin-checkbox` 和 `vaadin-select` 仍走 focused fallback，原因是 current app owner
在这些 focused imports 之前尚未 ready。

v1.39 的目标是把该 ordering blocker 往前推进：current app production build artifacts
必须优先于 covered component focused imports 被请求、stage，并触发 current app owner import；
组件 resolver 必须在 focused import 前消费这个 owner-ready 状态。

## 核心目标

新增 `flow-ipc-current-app-production-owner-early-import-v1_39` contract，覆盖：

- Early import metadata：Java manifest / resource response 声明 early import version、source、
  policy、phase、reusable tags、decision、failure layer 和 next step。
- Request ordering：Swift host 对 current app production build artifacts 的请求优先级必须高于
  covered component focused frontend resources。
- Owner-ready gate：covered component focused import 前，如果 current app owner early import 尚未
  完成但其资源已经可用，必须先触发 owner import。
- Partial reuse：`vaadin-checkbox`、`vaadin-select` 和 `vaadin-text-field` 应复用 current app owner
  已注册的 tags，并跳过对应 focused imports；`vaadin-big-decimal-field` 若 current app owner 不提供，
  必须分类为 owner-ready-but-unowned focused fallback，而不是继续写成 owner-not-ready。
- Visible Flow path：BigDecimalField、Checkbox 和 TextField 的 Java listener -> UIDL patch ->
  visible DOM 路径必须继续通过。

## Acceptable outcomes

- Preferred：Checkbox、Select 和 TextField 都 owner-reused；BigDecimalField 要么也复用 owner，
  要么明确记录 `current-app-owner-side-effect-does-not-own-tag-before-focused-import`。
- Acceptable：early import 在第一个 covered component focused import 前真实发生，但某些组件仍无法复用；
  trace 必须把 blocker 收敛到 owner side-effect tag coverage、custom element compatibility 或
  supported Vaadin build SPI，不得继续使用 v1.38 的 generic not-ready ordering blocker。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不删除 BigDecimalField、Checkbox、Select 或 TextField 的覆盖。
- 不引入 localhost/Vite dev server 作为成功证据。
- 不把 repository fixture 的 owner success 计为 current app owner success。
- 不要求一次性解决 dev-mode/Vite parity 或 broad component/resource parity。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39-current-app-owner-early-import .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_39.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39-current-app-owner-early-import docs/traces/flow-ipc-v1_39.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39 docs/traces/flow-ipc-v1_39.ndjson
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
feat: import current app owner before component fallbacks
```

## 完成记录

v1.39 已实现 `flow-ipc-current-app-production-owner-early-import-v1_39`：

- Java manifest / resource response 声明 early-import version、source、policy、phase、
  covered/reusable tags、decision、failure layer、next step 和 no-localhost/no-fixture proof。
- Swift host 将 current app production build artifact 请求优先级提升到 covered component
  focused imports 之前，并在 focused component 执行前等待/触发 current app owner import。
- Trace 证明 `currentAppProductionOwnerEarlyImportRequestBeforeCoveredComponents=true`、
  `currentAppProductionOwnerEarlyImportAttempted=true`、
  `currentAppProductionOwnerEarlyImported=true`。
- 当前真实结果是 acceptable outcome，不是 preferred reuse：
  `currentAppProductionOwnerEarlyImportDecision=early-import-owner-imported-before-components-side-effect-not-reusable`，
  `currentAppProductionOwnerEarlyImportSideEffectSatisfied=false`，
  `currentAppProductionOwnerEarlyImportPartialReuseSatisfied=false`。
- BigDecimalField、Checkbox、Select 和 TextField 仍走 focused fallback，但 blocker 已从
  v1.38 的 owner-not-ready ordering 层收敛为
  `current-app-owner-imported-before-focused-import-without-reusable-side-effect`。
- 可见 Flow path 继续通过：Clicks、BigDecimalField、Checkbox、TextField 的 Java listener ->
  UIDL patch -> visible DOM 均由真实窗口 smoke 验证。

固定 trace：`docs/traces/flow-ipc-v1_39.ndjson`。

完成验证：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39-current-app-owner-early-import docs/traces/flow-ipc-v1_39.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39 docs/traces/flow-ipc-v1_39.ndjson
```

仍未完成：完整 frontend/resource pipeline parity。下一阶段应解决 current app production owner
side-effect tag coverage/custom-element compatibility/supported Vaadin SPI，或转向真实
generated chunk、dev-mode/Vite、broader component parity 中的一个产品化 blocker。
