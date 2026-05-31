# Goal 1.46: Broader Component Resource Matrix Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.45 已经证明 packaging runtime handoff
不是 strict smoke app 专用：同一 `flow-ipc-packaging-runtime-handoff-reuse-v1_45`
contract 可以在 strict smoke app 和 framework validation app 上通过固定 trace gate。

v1.46 的目标是把该 handoff 推向更广组件/资源矩阵：在 framework-shaped validation app 中加入
更接近真实业务应用的 Flow 组件输入，验证 packaging-time `build-frontend` artifacts、generated
imports owner runtime contract 和 host-owned no-localhost resource loader 能否承载这些组件；如果
不能，必须把 blocker 收敛到具体 component tag、artifact、specifier、resource family 和 failure layer。

## 核心目标

新增 `flow-ipc-broader-component-resource-matrix-v1_46` contract，覆盖：

- Framework component matrix：在 `test-apps/flow-over-ipc-framework-app` 中加入普通 Flow
  `ComboBox`、`DatePicker` 和 `Dialog` 输入；如果实现成本可控，可加入只读小数据集 `Grid` /
  `VirtualList` 作为 classified probe，但不要求一次性达成完整 Grid parity。
- Resource graph evidence：trace 记录目标 component tags、Java classes、module specifiers、
  selected current-app source/prod artifacts、generated imports owner source、resolved/unresolved
  specifiers、custom-scheme / Blob scheme、sha256 / byte count 和 no-localhost proof。
- Runtime behavior：优先验证 ComboBox / DatePicker / Dialog 的真实可见 DOM、用户事件 ->
  Flow client -> IPC-backed UIDL RPC -> Java listener -> UIDL patch -> visible DOM 链路；若某组件
  被当前 pipeline 阻塞，必须记录 exact blocker，而不是用 smoke-only fallback 或 localhost 兜底。
- Trace gates：新增 framework matrix gate，至少验证 v1.45 handoff 仍成立、framework app 原有
  Button/BigDecimalField/Checkbox/Select/capability path 不回退，并新增组件矩阵的成功或分类 blocker。

## Acceptable outcomes

- Preferred：ComboBox、DatePicker 和 Dialog 都通过 no-localhost packaging runtime handoff 执行，
  并完成可见事件 / patch / DOM 验证。
- Acceptable：部分组件未能执行，但 trace 明确记录 component tag、resource artifact、
  unresolved specifier、browser/native error、failure layer、no-localhost/no-fixture proof 和下一步；
  已有 v1.45 handoff、framework interaction 和 capability path 不能回退。

## 不做什么

- 不声明 broad component/resource parity 完成。
- 不一次性解决 Upload/Download、复杂 Grid virtualization、Binder validation、theme variants。
- 不引入 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不把 focused smoke-only component fallback 当作 framework component matrix 成功。
- 不声明 installer packaging、dev-mode/Vite parity 或 durable reconnect 完成。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_46-broader-component-resource-matrix .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_46-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_46-broader-component-resource-matrix docs/traces/flow-ipc-v1_46-framework.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-framework-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: add broader component resource matrix contract
```

## 完成记录

- `test-apps/flow-over-ipc-framework-app` 新增普通 Flow `ComboBox`、`DatePicker` 和
  `Dialog`，并通过可见 Flow client -> IPC-backed UIDL RPC -> Java listener -> UIDL patch ->
  visible DOM 链路验证 `Reviewer changed: desktop`、`Due date changed: 2026-06-16`
  和 `Dialog opened: desktop due 2026-06-16`。
- Java manifest/resource response、Swift `resourceStage` / `webview.dom` 和 inspector gate
  新增 `flow-ipc-broader-component-resource-matrix-v1_46` contract，记录目标 tags/classes/ids、
  current-app build artifacts、generated imports owner source、resolved specifiers、custom-scheme
  loader、sha256/byte count、no-localhost/no-fixture proof 和 remaining blockers。
- no-HTTP Flow runtime session 现在在 route/component construction 和 UIDL writer 期间绑定
  desktop-shaped `UI` / request / response context，并提供最小 `VaadinContext` lookup，以支持
  Dialog/ComboBox 等更复杂组件路径。
- 固定 trace 已保存到 `docs/traces/flow-ipc-v1_46-framework.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_46-broader-component-resource-matrix .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_46-broader-component-resource-matrix docs/traces/flow-ipc-v1_46-framework.ndjson
```
