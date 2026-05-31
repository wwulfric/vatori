# Goal 1.45: Packaging Runtime Handoff Reuse Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.44 已经把 packaging-time `build-frontend`
产物选择、校验、stage、cache、cleanup 和 generated imports owner runtime handoff 记录为
`flow-ipc-packaging-time-frontend-inventory-v1_44`。但它仍偏向 strict smoke trace contract。

v1.45 的目标是把这个 inventory 推进为可复用 runtime handoff contract：trace 必须证明该 handoff
基于 module-relative build output，而不是 smoke-app-only 假设；同一 contract 需要在 strict smoke app
和 framework-shaped validation app 上都能被验收。

## 核心目标

新增 `flow-ipc-packaging-runtime-handoff-reuse-v1_45` contract，覆盖：

- Module-relative handoff：trace 记录 module path、app shape、artifact base policy、selected artifacts
  和 runtime contracts，证明输入来自当前 app 的 source/target build output。
- Reuse semantics：trace 记录 reusable contract、non-smoke-specific、same-contract-as-inventory、
  host-owned loader/custom-scheme required、runtime handoff ready/satisfied。
- Framework validation：除 strict smoke app 外，framework validation app 也必须能用同一 v1.45
  contract 完成真实窗口 interaction、component events、patch 和 desktop capability path。
- Trace gates：新增 smoke/app-shape gate 和 framework app gate，避免把单个 smoke log 误读成可复用性。

## 不做什么

- 不声明完整 installer packaging、signing 或 update。
- 不声明 dev-mode/Vite parity。
- 不扩展到 Grid、ComboBox、Dialog、Upload 等大组件矩阵。
- 不把 host audit probe 包装成 supported Flow-client lazy chunk SPI 成功。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-packaging-runtime-handoff-reuse .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45 .build/vaadin-desktop-host-swift/host.out.log
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_45-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse docs/traces/flow-ipc-v1_45-framework.ndjson
```

另需保存 strict smoke app trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_45.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-packaging-runtime-handoff-reuse docs/traces/flow-ipc-v1_45.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45 docs/traces/flow-ipc-v1_45.ndjson
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
- `test-apps/flow-over-ipc-framework-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: add packaging runtime handoff reuse contract
```

## 完成记录

- Java manifest/resource response now emits v1.45 module-relative packaging runtime handoff
  reuse fields for strict smoke, framework validation and ordinary developer app shapes.
- Swift host propagates the v1.45 contract into `resourceStage` / `webview.dom`, records
  runtime handoff ready/satisfied evidence, and extends framework auto-verification timing
  so build-frontend resource staging does not race framework component interactions.
- `DesktopTraceInspector` adds strict smoke and framework gates for
  `flow-ipc-packaging-runtime-handoff-reuse-v1_45`.
- Framework validation app now includes the same v1.27 lazy real-build probe input shape, so
  its explicit `build-frontend` output can produce current-app generated imports/chunk evidence.
- Fixed traces are saved as `docs/traces/flow-ipc-v1_45.ndjson` and
  `docs/traces/flow-ipc-v1_45-framework.ndjson`.

已执行验收：

```bash
git diff --check
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow,test-apps/flow-over-ipc-smoke-app,test-apps/flow-over-ipc-framework-app -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-packaging-runtime-handoff-reuse docs/traces/flow-ipc-v1_45.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45 docs/traces/flow-ipc-v1_45.ndjson
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse docs/traces/flow-ipc-v1_45-framework.ndjson
```
