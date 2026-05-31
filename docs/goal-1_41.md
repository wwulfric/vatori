# Goal 1.41: Current App Generated Imports Select Owner Coverage

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.40 已证明当前 app production
`generated-flow-imports-*.js` support artifact 可以在 covered component focused imports
之前通过 host-owned `vatori-resource://` 导入，并复用
`vaadin-big-decimal-field`、`vaadin-checkbox` 和 `vaadin-text-field`。但 `vaadin-select`
仍是明确 fallback；这说明 strict smoke app 的当前 app build output 还没有把 Select 纳入
同一 owner side-effect 路径。

## 核心目标

新增 `flow-ipc-current-app-generated-imports-select-owner-coverage-v1_41` contract，覆盖：

- strict smoke app 通过普通 Flow `Select` 组件和 `vaadin-select-flow` 依赖声明真实 Select
  输入，而不是只在 framework app 中验证 Select。
- 当前 app full `build-frontend` 产物中的 production `generated-flow-imports-*.js` support
  artifact 必须注册 `vaadin-select`，并继续注册
  `vaadin-big-decimal-field`、`vaadin-checkbox` 和 `vaadin-text-field`。
- Swift host 的 generated imports owner side-effect diagnostics 必须把 expected / observed /
  reused tags 收敛到四组件完整覆盖，`knownMissingTags` 和 fallback tags 为空。
- Component resolver 必须跳过四个 covered focused imports，包括
  `META-INF/vatori/dev-bundle-entry/vaadin-select.js`。
- 可见 Flow path 必须继续通过：Button、BigDecimalField、Checkbox、TextField、Select、
  resync、file dialog cancel 和 error reporting 不能回退。

## Acceptable outcomes

- Preferred：current app generated imports owner side effect 导入成功，四个 covered component
  tags 全部 observed/reused，四个 focused imports 全部 skipped，并且可见 Select
  change event 经 Flow client -> IPC-backed UIDL RPC -> Java listener -> UIDL patch -> DOM
  更新为 `Vaadin Select: audit`。
- Acceptable：如果 `vaadin-select` 在当前 app generated imports support artifact 中仍无法注册，
  trace 必须证明 strict smoke app 的 Select 输入、build output、artifact body、browser error、
  registry/global diagnostics 和具体 failure layer，不能继续停在 v1.40 的
  `vaadin-select` fallback 分类。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 framework app 的 Select 路径计为 strict smoke app 的 current app owner coverage。
- 不引入 localhost/Vite dev server 作为成功证据。
- 不一次性解决 dev-mode/Vite parity、Windows/Linux host、installer packaging 或 broad component matrix。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41-current-app-generated-imports-select-owner-coverage .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_41.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41-current-app-generated-imports-select-owner-coverage docs/traces/flow-ipc-v1_41.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41 docs/traces/flow-ipc-v1_41.ndjson
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
feat: cover select with generated imports owner
```

## 完成记录

- strict smoke app 新增普通 Flow `Select` 和 `vaadin-select-flow` 依赖，Select change event
  通过 Flow client -> IPC-backed UIDL RPC -> Java listener -> UIDL patch 更新 DOM 为
  `Vaadin Select: audit`。
- current app production `generated-flow-imports-*.js` support artifact 通过 host-owned
  `vatori-resource://` side-effect path 注册并复用 `vaadin-big-decimal-field`、
  `vaadin-checkbox`、`vaadin-select` 和 `vaadin-text-field`。
- Swift host 的聚合诊断在四个 covered tags 都 reused/skipped 后归一化为
  `generated-imports-owner-reused-covered-tags`，避免最后一次 focused-import skip decision
  遮蔽完整覆盖结论。
- 固定 trace 已保存到 `docs/traces/flow-ipc-v1_41.ndjson`。

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41-current-app-generated-imports-select-owner-coverage .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41-current-app-generated-imports-select-owner-coverage docs/traces/flow-ipc-v1_41.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41 docs/traces/flow-ipc-v1_41.ndjson
```
