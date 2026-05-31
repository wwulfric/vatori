# Goal 1.48: Grid Lazy Data Provider Viewport RPC Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.47 已经把 framework validation app
扩展到 bounded data / validation matrix，覆盖普通 Flow `Grid`、`VirtualList`、
`FormLayout` 和 Binder-backed `TextField` validation。

v1.48 的目标是在这条 framework-shaped app 路径上继续深入 Grid 行为：验证真实
Flow `Grid` 的 callback-backed lazy data provider 与 published server method
`setViewportRange` 可以通过 Flow client -> IPC XHR bridge -> Java DataProvider fetch ->
UIDL patch -> visible DOM 闭环工作。这个目标不声明完整滚动虚拟化，只证明当前 no-localhost
resource/runtime handoff 已经能承载一个真实 Grid viewport RPC 和后续 lazy fetch。

## 核心目标

新增 `flow-ipc-grid-lazy-data-provider-v1_48` contract，覆盖：

- Framework app：在 `test-apps/flow-over-ipc-framework-app` 中加入 callback-backed
  `Grid<WorkItem>`，记录 lazy fetch 次数、count callback 次数和最后 fetch range。
- Published RPC：Swift host 通过真实 Flow client published method 调用
  `framework-lazy-grid.$server.setViewportRange(20, 8)`，并让 Java DataProvider 产生
  normalized `15-20` callback fetch。
- Runtime behavior：自动点击 `Report Lazy Grid`，验证 report patch 把
  `Lazy grid fetches: 10, count calls: 1, range: 15-20` 更新到 visible DOM。
- Resource evidence：manifest / resource response / `resourceStage` / `webview.dom`
  记录目标 tags/classes/ids、module specifier、current-app source/prod artifacts、
  generated imports owner source、sha256 / byte count、no-localhost / no-fixture proof 和
  remaining blockers。
- Trace gates：新增 `--require-flow-ipc-v1_48-grid-lazy-data-provider` 和
  `--require-flow-ipc-v1_48`，并要求 v1.47 data component matrix 不回退。

## Acceptable outcomes

- Preferred：`setViewportRange(20, 8)` 经 published RPC 到达 Java，DataCommunicator 按
  Vaadin Grid page/cache 语义归一化并触发 `15-20` callback fetch，随后 report click
  更新 visible DOM。
- Acceptable：Grid 内部 range 与请求 range 不逐字相同，但 trace 必须同时记录请求参数、
  callback-backed fetch range、report patch 和 DOM completion。v1.47 data/Binder matrix 不能回退。

## 不做什么

- 不声明完整 Grid scroll virtualization 或所有 `DataCommunicator` 行为完成。
- 不实现 Upload/Download streaming、theme variants、dev-mode/Vite parity、installer packaging 或
  durable reconnect。
- 不使用 localhost/Vite dev server 作为 Flow IPC 成功证据。
- 不把初始 Grid fetch `0-5` 单独当成 v1.48 成功；必须有 published RPC 和后续 report DOM。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_48-grid-lazy-data-provider .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_48-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_48-grid-lazy-data-provider docs/traces/flow-ipc-v1_48-framework.ndjson
```

## 完成记录

- Framework validation app 新增 `framework-lazy-grid`、`framework-lazy-grid-status` 和
  `framework-lazy-grid-report`，用普通 Flow `Grid` + callback `setItems(fetch, count)` 记录
  lazy DataProvider 行为。
- Java manifest/resource metadata 新增
  `flow-ipc-grid-lazy-data-provider-v1_48`，记录 Grid lazy provider 的 target tags/classes/ids、
  `setViewportRange` published method、requested viewport `20:8`、normalized expected result、
  selected artifacts、integrity、no-localhost/no-fixture proof 和 remaining blockers。
- Swift host 新增 `VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1` 自动验证路径，支持 Flow
  published server method RPC，验证 `setViewportRange(20, 8)` 和 report click 的 UIDL patch/DOM。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_48-grid-lazy-data-provider` 和 `--require-flow-ipc-v1_48` gates；固定
  framework trace 保存为 `docs/traces/flow-ipc-v1_48-framework.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_48-grid-lazy-data-provider .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_48-grid-lazy-data-provider docs/traces/flow-ipc-v1_48-framework.ndjson
```

v1.48 是 Grid lazy DataProvider viewport RPC 的 bounded contract，不声明完整 Grid
scroll virtualization、Upload/Download、theme variants、dev-mode/Vite、installer packaging 或
durable reconnect parity。
