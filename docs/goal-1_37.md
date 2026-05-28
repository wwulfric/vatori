# Goal 1.37: Current App Production Owner Component Reuse

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.36 已证明 current app production owner 可以通过
host-owned `vatori-resource://` custom scheme import，并且观察到 `window.Vaadin` /
`window.Vaadin.Flow.loadOnDemand` / expected custom elements side effects。但组件 resolver
仍然没有消费这个 owner side effect：后续 `vaadin-text-field` 仍走 focused dev-bundle import。

v1.37 的目标是把 v1.36 的 current app production owner side-effect proof 接入真实组件复用决策：
当 current app owner 已经注册目标 custom element，后续 focused component import 必须可以跳过，并在
trace 中记录 owner-owned reuse，而不是继续把它当作普通 focused fallback。

## 核心目标

新增 `flow-ipc-current-app-production-owner-component-reuse-v1_37` contract，覆盖：

- Reuse metadata：Java manifest / resource response 声明 reuse version、source、policy、
  expected tags、failure layer 和 next step。
- WebView decision：Swift host 在 focused component import 前检查 current app production owner
  import side effect；若目标 tag 已由 owner 注册，则跳过 focused import。
- TextField reuse proof：`META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` 应记录为
  `current-app-owner-reused`，并保持真实 Flow `TextField` Java listener -> UIDL patch -> visible DOM
  更新。
- Blocker narrowing：如果只能复用 TextField，trace 必须明确它不是 Checkbox/Select/BigDecimalField
  的泛化成功；下一步继续处理更早组件执行顺序、dev-mode/Vite parity 和更广组件覆盖。

## Acceptable outcomes

- Preferred：current app production owner import side effects 被组件 resolver 复用，TextField focused
  dev-bundle import 被跳过，trace 记录 current app owner-owned tag、skipped focused import 和 no-localhost proof。
- Acceptable：复用检查真实发生但目标 tag 不可复用；trace 记录 current-app-owner-specific failure layer，
  不回退到 v1.36 的 generic `currentAppProductionOwnerImportReuseSatisfied=false`。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 v1.23 repository production owner positive fixture 当作 current app owner reuse。
- 不要求 v1.37 一次性让 Checkbox/Select/BigDecimalField 全部复用 current app owner；若执行顺序仍要求
  focused fallback，必须如实记录。
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
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37-current-app-production-owner-component-reuse .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_37.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37-current-app-production-owner-component-reuse docs/traces/flow-ipc-v1_37.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37 docs/traces/flow-ipc-v1_37.ndjson
```

## 完成记录

v1.37 已将 v1.36 的 current app production owner side-effect proof 接入 TextField 组件复用决策：

- Java manifest / resource response 声明 `flow-ipc-current-app-production-owner-component-reuse-v1_37`
  metadata、复用策略、expected tag、failure layer 和 next step。
- Swift host 在 focused `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` import 前检查 current app
  production owner side effect；当 owner 已注册 `vaadin-text-field` 时，跳过 focused import。
- Trace 记录 `currentAppProductionOwnerComponentReuseDecision=current-app-owner-reused`、
  `currentAppProductionOwnerComponentReused=true`、
  `currentAppProductionOwnerComponentReuseReusedTags=vaadin-text-field` 和
  `currentAppProductionOwnerComponentReuseSkippedFocusedImports=META-INF/vatori/dev-bundle-entry/vaadin-text-field.js`。
- 真实窗口 smoke 仍验证可见 Flow `TextField` Java listener -> UIDL patch -> DOM
  `Vaadin TextField: Flow IPC typed`。
- `--require-flow-ipc-v1_37` 当前作为 latest/delta alias，仅验证 v1.37 的最新切片；它不是从 v1.0
  到 v1.36 的历史累计 gate。

剩余 blocker：更早组件执行顺序的泛化 owner reuse、真实 app/build generated chunk-file parity、
dev-mode/Vite execution 和 broad component/resource parity。

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
feat: reuse current app production owner components
```
