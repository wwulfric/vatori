# Goal 1.36: Current App Production Owner Import Attempt

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.35 已经把 current app `build-frontend`
输出收束成 handoff-ready runtime candidate，但没有证明 production owner import、side-effect
或 component reuse 成功。

v1.36 的目标是把 v1.35 candidate 接入真实 current-app production owner import attempt：
用 host-owned custom scheme stage 当前 app production entry 和 dependency graph，执行一次受控
dynamic import，并把 import result、side-effect proof、reuse decision 和 failure layer 写入 trace。

## 核心目标

新增 `flow-ipc-current-app-production-owner-import-v1_36` contract，覆盖：

- Owner import candidate：记录 current app production entry、support artifact、dependency graph、
  expected side effects 和 handoff-ready 状态。
- Custom-scheme attempt：当前 app production artifacts 通过 `vatori-resource://` stable source path
  stage/import，不使用 localhost，也不使用 v1.23 repository fixture。
- Side-effect proof：记录 registry/global before/after、import result、browser error、
  side-effect satisfied 和 reuse satisfied。
- Blocker narrowing：如果 import 或 side-effect 仍失败，必须落到
  `current-app-production-owner-*` failure layer，而不是回退到 v1.35 handoff-pending。

## Acceptable outcomes

- Preferred：current app production owner import resolves and records reusable side effects / component reuse.
- Acceptable：import attempt 真实发生但 side-effect/reuse 仍不满足；trace 记录 artifact、custom-scheme、
  browser/native error、failure layer、no-localhost 和 no-fixture proof。

实际完成结果：current app production owner import 已通过 host-owned `vatori-resource://`
custom scheme 执行；trace 记录 root/dependency staging、`currentAppProductionOwnerImported=true`、
`currentAppProductionOwnerImportSideEffectSatisfied=true` 和可见 side-effect proof。component reuse
仍为 `currentAppProductionOwnerImportReuseSatisfied=false`，因此 v1.36 是 owner import /
side-effect proof，不是完整 frontend/resource pipeline 闭环。

## 不做什么

- 不声明完整 frontend/resource pipeline 闭环。
- 不把 v1.23 repository production owner positive fixture 当作 current app production owner 成功。
- 不引入 localhost/Vite dev server 作为成功证据。
- 不把 current app generated root/chunk execution 继续包装成 owner side-effect success。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36-current-app-production-owner-import .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_36.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36-current-app-production-owner-import docs/traces/flow-ipc-v1_36.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36 docs/traces/flow-ipc-v1_36.ndjson
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
feat: add current app production owner import attempt
```

## 完成记录

- Swift host 在 current app production root/dependency bodies 都就绪后触发 owner import attempt，
  并沿用 v1.35 handoff metadata 生成 v1.36 `resourceStage` / `webview.dom` 证据。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_36-current-app-production-owner-import` 和
  `--require-flow-ipc-v1_36` gates；v1.35 successor evidence 允许 owner side-effect boolean 在
  v1.36 trace 中从 false 推进为 true。
- 固定 trace 已保存到 `docs/traces/flow-ipc-v1_36.ndjson`。

已执行验收：

```bash
git diff --check
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36-current-app-production-owner-import .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36 .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36-current-app-production-owner-import docs/traces/flow-ipc-v1_36.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36 docs/traces/flow-ipc-v1_36.ndjson
```
