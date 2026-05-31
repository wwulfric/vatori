# Goal 1.49: Grid Client Scroll Virtualization Probe Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.48 已证明 framework validation app 中
callback-backed lazy `Grid` 可以通过直接 published server method
`setViewportRange(20, 8)` 触发真实 Java DataProvider callback、UIDL patch 和 visible DOM。

v1.49 的目标是把这个边界向真实 Grid virtualization 再推进一步：不再把直接
`$server.setViewportRange(...)` 当作新的成功证据，而是由 host/WebView 触发真实
Vaadin Grid client API `scrollToIndex(45)`，观察它是否通过 Flow client / Grid connector
自然发出 `setViewportRange` published RPC，并让 callback DataProvider fetch、report patch 和
visible DOM 形成可审计 trace contract。

这个目标仍然不声明完整 Grid scroll virtualization；它只验证一个 client scroll API probe。

## 核心目标

新增 `flow-ipc-grid-scroll-virtualization-v1_49` contract，覆盖：

- Framework app：复用 v1.48 的 `framework-lazy-grid` callback-backed lazy Grid。
- Client API trigger：Swift host / WebView 自动验证路径调用
  `framework-lazy-grid.scrollToIndex(45)`，并记录 trigger source 为 `client-scroll-api`。
- RPC evidence：如果真实 Grid connector 发出 `setViewportRange` published RPC，trace 必须记录
  component id、method、requested index、observed fetch range 和 UIDL patch。
- Runtime behavior：自动点击 `Report Lazy Grid`，验证 report patch 把 latest lazy range 更新到
  visible DOM，并与 client scroll trigger 关联。
- Resource evidence：manifest / resource response / `resourceStage` / `webview.dom` 记录
  target tags/classes/ids、module specifier、current-app source/prod artifacts、
  generated imports owner source、sha256 / byte count、no-localhost / no-fixture proof 和
  remaining blockers。
- Trace gates：新增 `--require-flow-ipc-v1_49-grid-scroll-virtualization` 和
  `--require-flow-ipc-v1_49`，并要求 v1.48 Grid lazy DataProvider gate 不回退。

## Acceptable outcomes

- Preferred：`scrollToIndex(45)` 经真实 Grid client/connector path 触发
  `setViewportRange` published RPC，Java DataProvider fetch range 更新到 index 45 附近的
  Vaadin-normalized range，随后 report click 更新 visible DOM。
- Acceptable blocker：如果 `scrollToIndex(45)` 当前不会发出 RPC，trace 必须明确记录
  `client-scroll-api-no-viewport-rpc`、Grid element readiness、method existence、attempt count、
  current status text、browser error 和下一步拆解；不能把 fallback direct `$server` call 计入
  v1.49 成功。

## 不做什么

- 不声明完整 Grid scroll virtualization、所有 scroll container 状态或所有
  `DataCommunicator` 行为完成。
- 不实现 Upload/Download streaming、theme variants、dev-mode/Vite parity、installer packaging 或
  durable reconnect。
- 不使用 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不把 v1.48 的直接 `setViewportRange(20, 8)` 成功单独当成 v1.49 成功。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_49-grid-scroll-virtualization .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_49-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_49-grid-scroll-virtualization docs/traces/flow-ipc-v1_49-framework.ndjson
```

## 完成记录

真实窗口验证选择了 Preferred outcome。`scrollToIndex(45)` 在
`framework-lazy-grid` 上可用并被调用，Flow client / Grid connector 通过
`triggerSource=client-scroll-api` 发出 published viewport RPC，Java callback-backed
DataProvider 观察到 Vaadin 规范化后的 `60-65` lazy fetch range；随后
`Report Lazy Grid` click 经 UIDL patch 更新可见 DOM。

固定 trace：`docs/traces/flow-ipc-v1_49-framework.ndjson`

已通过 gate：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_49-grid-scroll-virtualization docs/traces/flow-ipc-v1_49-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_49 docs/traces/flow-ipc-v1_49-framework.ndjson
```

v1.49 仍然是 bounded client scroll API probe，不声明完整 Grid scroll virtualization、
所有 scroll container 状态、Upload/Download、theme variants、dev-mode/Vite、installer
packaging 或 durable reconnect parity。
