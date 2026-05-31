# Goal 1.47: Data Component and Binder Resource Matrix Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.46 已经把 packaging runtime handoff
扩展到 framework validation app 中的 ComboBox、DatePicker 和 Dialog，并证明它们的
可见事件 / UIDL patch / DOM 链路可以在 no-localhost Flow IPC runtime 下工作。

v1.47 的目标是继续扩大真实业务组件覆盖：把数据展示和表单校验组件纳入同一
packaging-time frontend inventory / generated imports owner / host-owned resource loader 证据链。
这不是完整 Grid virtualization、lazy data provider、Upload streaming 或 theme parity；它是一个
bounded data/validation matrix，用于证明更复杂组件族可以被当前 runtime 发现、stage、渲染并参与
server-side Flow 状态更新。

## 核心目标

新增 `flow-ipc-data-component-resource-matrix-v1_47` contract，覆盖：

- Framework data matrix：在 `test-apps/flow-over-ipc-framework-app` 中加入普通 Flow
  `Grid`、`VirtualList`、`FormLayout` 和 `Binder`/`TextField` 校验路径。
- Resource graph evidence：trace 记录目标 component tags、Java classes、component ids、
  module specifiers、current-app source/prod artifacts、generated imports owner source、
  resolved/unresolved specifiers、custom-scheme / Blob scheme、sha256 / byte count 和 no-localhost
  proof。
- Runtime behavior：验证 Grid / VirtualList / FormLayout 的可见 DOM 或明确分类 blocker；
  验证 Binder-backed TextField change -> validate Button click -> Java Binder validation ->
  UIDL patch -> visible DOM 链路。
- Trace gates：新增 framework data matrix gate，至少验证 v1.46 broader component matrix 不回退，
  并新增数据组件 / Binder matrix 的成功或分类 blocker。

## Acceptable outcomes

- Preferred：Grid、VirtualList、FormLayout 和 Binder 校验都通过 no-localhost packaging runtime
  handoff 执行，并完成可见 DOM / event / patch 验证。
- Acceptable：Grid 或 VirtualList 的深层数据/virtualization 行为未完全闭合，但 trace 必须明确记录
  component tag、resource artifact、unresolved specifier 或 runtime failure layer；Binder 校验路径和
  既有 v1.46 matrix 不能回退。

## 不做什么

- 不声明完整 Grid virtualization、lazy data provider、VirtualList item renderer parity 完成。
- 不实现 Upload/Download streaming、theme variants、dev-mode/Vite parity、installer packaging 或
  durable reconnect。
- 不用 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不把只存在于 smoke app 的 focused fallback 当作 framework data matrix 成功。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_47-data-component-resource-matrix .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_47-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_47-data-component-resource-matrix docs/traces/flow-ipc-v1_47-framework.ndjson
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
feat: add data component resource matrix contract
```

## 完成记录

- `test-apps/flow-over-ipc-framework-app` 新增普通 Flow `Grid`、`VirtualList`、
  `FormLayout` 和 `Binder`/`TextField` 校验路径；owner change 与 validate click 会通过
  Java Binder 更新可见 DOM 为 `Validation accepted: Ava Desktop`。
- Java manifest/resource metadata 新增
  `flow-ipc-data-component-resource-matrix-v1_47`，记录目标 tags/classes/ids、
  current-app source/prod artifacts、generated imports owner source、specifier resolution、
  integrity、no-localhost/no-fixture proof 和 remaining blockers。
- Swift host 新增 `VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1` 自动验证路径，并在
  `resourceStage` / `webview.dom` 中记录 data component matrix 的 rendered/event/patch/DOM
  完成证据。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_47-data-component-resource-matrix` 和
  `--require-flow-ipc-v1_47` gates；固定 framework trace 保存为
  `docs/traces/flow-ipc-v1_47-framework.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_47-data-component-resource-matrix .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_47-data-component-resource-matrix docs/traces/flow-ipc-v1_47-framework.ndjson
```

v1.47 是 bounded data / Binder component-resource matrix，不声明完整 Grid virtualization、
lazy data provider、Upload/Download、theme variants、dev-mode/Vite、installer packaging 或
durable reconnect parity。
