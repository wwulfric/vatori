# Goal 1.24: Frontend Pipeline Closure Decision Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.21 已统一 frontend resolver
contract，v1.22 已证明 generated `Flow.loadOnDemand` chunk-file table 存在时
no-localhost resolver 可以执行，v1.23 已证明 production owner custom-scheme
runtime 在具备 owner contract 时可以执行。

v1.24 的目标不是宣布真实 Vaadin production bundle / dev-mode 已完成，而是把这些
证据收束成一个可审计的 **frontend pipeline closure decision contract**：框架必须在
trace 中明确回答当前 frontend/resource pipeline 哪些 family 已闭合、哪些是 fixture
positive proof、哪些仍是当前 app/build 真实 blocker，以及下一步产品化路径选哪个。

## 核心目标

新增 `flow-ipc-frontend-pipeline-closure-v1_24` contract，覆盖：

- no-localhost IPC resource transport。
- v1.21 unified resolver contract。
- v1.22 generated chunk-file positive fixture。
- v1.23 production owner positive fixture。
- 当前真实 app/build 的 generated chunk、production owner、dev-mode/Vite blocker。
- 一个明确的 product path decision：`packaging-time-staged-frontend-or-supported-vaadin-build-spi`。

v1.24 的完成条件是：trace inspector 可以证明框架已经具备“基础设施闭环 + 真实
app/build 差距分类 + 下一步路径决策”的合同证据，而不是只靠分散的历史 gate。

## 不做什么

- 不把 v1.22 fixture 包装成当前 app/build 已生成真实 lazy chunk。
- 不把 v1.23 fixture 包装成当前 app/build production owner parity。
- 不要求 dev-mode/Vite execution 本阶段完成。
- 不扩大到 Grid/ComboBox/DatePicker/Dialog/Upload。
- 不把长期 `docs/goal.md` 标记为完成。

## 实现要求

### Java runtime metadata

`FlowClientResources` 的 flow-client manifest、component manifest、resource response
都要带 v1.24 metadata：

- `frontendPipelineClosureVersion=flow-ipc-frontend-pipeline-closure-v1_24`
- `frontendPipelineClosureScope=flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode`
- `frontendPipelineClosureTransport=no-localhost-ipc-resource-request`
- `frontendPipelineClosureInfrastructureStatus=closed-for-representative-fixture-paths`
- `frontendPipelineClosureRealBuildStatus=product-parity-pending`
- `frontendPipelineClosureSatisfiedProofs=flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed`
- `frontendPipelineClosureRemainingBlockers=real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped`
- `frontendPipelineClosureDecision=basic-pipeline-infrastructure-closed-real-build-parity-pending`
- `frontendPipelineClosureNextProductPath=packaging-time-staged-frontend-or-supported-vaadin-build-spi`
- `frontendPipelineClosureNoLocalhost=true`

### Swift host / WebView diagnostics

Swift host / WebView 要把 v1.24 metadata 传播到：

- `resourceRequest`
- `resourceResponse`
- `resourceStage`
- `webview.dom`

DOM summary 必须能同时证明：

- v1.22 generated chunk fixture 成功。
- v1.23 production owner positive fixture 成功。
- 真实 app/build blocker 仍被保留，而不是被 fixture 覆盖。
- `frontendPipelineClosureDecision` 明确不是 full production/dev-mode parity。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 docs/traces/flow-ipc-v1_24.ndjson
```

`--require-flow-ipc-v1_24-frontend-pipeline-closure` 只验证 v1.24 delta；
`--require-flow-ipc-v1_24` 继承 v1.23 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23 docs/traces/flow-ipc-v1_23.ndjson
```

## 完成后更新

- `docs/goal.md`
- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/acceptance-tools.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/flow-transport-adapter.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/ipc-protocol-v1.md`
- `docs/native-host.md`
- `docs/roadmap.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`

完成提交必须使用 Conventional Commit，例如：

```text
feat: add frontend pipeline closure gate
```

## 完成记录

v1.24 已实现并固定为 `docs/traces/flow-ipc-v1_24.ndjson`。完成证据：

- `git diff --check`
- `bash -n bin/vaadin-desktop`
- `swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift`
- `source /Users/bytedance/.sdkman/bin/sdkman-init.sh && mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile`
- `bash bin/flow-client-resource-smoke`
- `VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app`
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure .build/vaadin-desktop-host-swift/host.out.log`
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 .build/vaadin-desktop-host-swift/host.out.log`
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure docs/traces/flow-ipc-v1_24.ndjson`
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 docs/traces/flow-ipc-v1_24.ndjson`
- `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23 docs/traces/flow-ipc-v1_23.ndjson`

完成后的明确边界仍然成立：v1.24 证明的是 no-localhost frontend/resource
pipeline 的代表性基础设施闭环、真实 app/build blocker 分类和下一步产品路径决策；
它不是完整 Vaadin production bundle / dev-mode parity。
