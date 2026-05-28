# Goal 1.32: Current App Build-Frontend Output Contract

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.31 已完成
`flow-ipc-frontend-build-preflight-availability-v1_31`：显式 online preflight 能用项目本地
Maven repo 解析并运行 `com.vaadin:vaadin-maven-plugin:25.1.5:prepare-frontend`，但输出仍是
scaffold，没有当前 app 的 usable generated imports/chunks。

v1.32 的目标是把 blocker 从 `prepare-frontend` 层推进到完整 Vaadin
`build-frontend` / package 输出层：让 `dev-ipc` 能显式运行 full frontend build，审计当前 app
真实生成的 source generated imports/chunk、production webapp build artifacts 和
`src/main/bundles/prod.bundle`，并把这些输出作为 Flow IPC no-localhost resource pipeline 的候选
输入记录到 trace。

## 核心目标

新增 `flow-ipc-current-app-build-frontend-output-v1_32` contract，覆盖：

- Build goal selection：`dev-ipc` frontend build preflight 可显式选择
  `prepare-frontend` 或 `build-frontend`，并记录 goal、production flags、effective Maven mode、
  command、status、logs 和 output usability。
- Source generated output：识别
  `src/main/frontend/generated/flow/generated-flow-imports.js` 和
  `src/main/frontend/generated/flow/chunks/chunk-*.js`，记录 table shape、chunk id、chunk file、
  sha256、bytes 和是否来自当前 app build。
- Production output：识别
  `target/classes/META-INF/VAADIN/webapp/VAADIN/build/*.js`、`config/stats.json` 和
  `src/main/bundles/prod.bundle`，记录 selected entry、generated imports artifact、bundle size、
  bundle sha256 和当前可执行性分类。
- Runtime decision：当前 app source/prod output 可用时，metadata 必须明确优先级和剩余 blocker；
  packaged dev-bundle fallback 只能作为旧路径回归保护，不能被计为 v1.32 成功。

## Acceptable outcomes

- Preferred：`build-frontend` 产生真实当前 app generated imports 和 chunk file，no-localhost
  resolver 能 stage/import 当前 app generated imports，并触发 chunk callback；若 chunk 内仍有
  bare npm specifier 或 production owner blocker，trace 必须记录精确 failure layer。
- Acceptable：`build-frontend` 成功且当前 app source generated imports/chunk、production webapp
  output、`prod.bundle` 都被发现和审计，但执行仍停在明确层，例如
  `current-app-source-generated-chunk-npm-specifier-unresolved` 或
  `current-app-production-generated-imports-owner-boundary`。
- Acceptable：`build-frontend` 因 Node/npm/plugin/build 配置失败；trace 必须记录失败命令、
  failure layer 和下一步，不能回退成 v1.31 的 `frontend-build-plugin-resolved-output-incomplete`。

## 不做什么

- 不把 packaged Vaadin dev-bundle jar artifact 当作当前 app build output。
- 不把 v1.22 repository fixture 当作当前 app generated chunk parity。
- 不把普通 `LoadMode.LAZY` script 当作 generated `Flow.loadOnDemand(chunkId)` chunk parity。
- 不要求默认启用 online build；full build 仍需显式 opt-in，避免日常 `dev-ipc` 启动变慢或写入外部
  Node/npm cache。
- 不声明完整 frontend/resource pipeline 闭环，除非当前 app generated/prod artifact 真正经
  no-localhost resolver 被 Flow client 触发并执行。

## 实现要求

### CLI / build preflight

- 增加显式 build goal，例如：
  `VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend`。
- `build-frontend` 模式使用项目本地 Maven repo，并记录：
  - selected goal
  - production mode / force production build / frontend version check flags
  - output roots
  - stdout/stderr log
  - output usability
- 继续保留 `prepare-frontend` 作为 v1.31 回归路径。

### Java resolver / metadata

- `FlowClientResources` 增加 v1.32 metadata：
  - `currentAppBuildFrontendOutputVersion=flow-ipc-current-app-build-frontend-output-v1_32`
  - `currentAppBuildFrontendGoal=<prepare-frontend|build-frontend>`
  - `currentAppBuildFrontendSourceGeneratedImportsObserved=<true|false>`
  - `currentAppBuildFrontendSourceChunkStatus=<available|not-found|not-generated|missing>`
  - `currentAppBuildFrontendProductionBuildObserved=<true|false>`
  - `currentAppBuildFrontendProductionGeneratedImportsPath=<path>`
  - `currentAppBuildFrontendProdBundleObserved=<true|false>`
  - `currentAppBuildFrontendRuntimeCandidate=<source-generated-imports|production-webapp-build|none>`
  - `currentAppBuildFrontendFailureLayer=<classification>`
- 如果当前 app source generated imports/chunk 可用，manifest 中应把它作为 generated-lazy
  resource candidate 暴露出来，供 host 通过同一 IPC resource request/staging path 请求。
- 记录 chunk body 内是否仍存在 bare npm imports；如果存在，failure layer 必须收敛到 npm specifier
  resolver，而不是说 chunk 文件缺失。

### Swift host / WebView diagnostics

- 将 v1.32 metadata 传播到 `resourceRequest`、`resourceResponse`、`resourceStage` 和
  `webview.dom`。
- 如果当前 app generated-lazy resource 被请求，stage outcome 必须记录 runtime candidate、
  source/prod path、chunk status 和 failure layer。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32-build-frontend-output docs/traces/flow-ipc-v1_32.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32 docs/traces/flow-ipc-v1_32.ndjson
```

`--require-flow-ipc-v1_32-build-frontend-output` 只验证 v1.32 delta；
`--require-flow-ipc-v1_32` 继承 v1.31 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32-build-frontend-output .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_32.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32-build-frontend-output docs/traces/flow-ipc-v1_32.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32 docs/traces/flow-ipc-v1_32.ndjson
```

## 完成记录

v1.32 已完成 `flow-ipc-current-app-build-frontend-output-v1_32` contract：

- `dev-ipc` 支持
  `VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_GOAL=build-frontend`，并记录 production mode、
  force production build、frontend version check flags、effective Maven mode、command/status/log
  和 output usability。
- 当前 smoke app 的 full Vaadin frontend build 产生了真实
  `src/main/frontend/generated/flow/generated-flow-imports.js` 和
  `src/main/frontend/generated/flow/chunks/chunk-*.js`，同时产生 production webapp build output
  和 `src/main/bundles/prod.bundle`。
- Java resolver 把当前 app source generated imports/chunk 暴露为 generated-lazy resource
  candidate；Swift host 在 `resourceRequest`、`resourceResponse`、`resourceStage` 和
  `webview.dom` 中传播 v1.32 metadata。
- 当前 accepted blocker 已从 v1.31 的
  `frontend-build-plugin-resolved-output-incomplete` 推进到
  `current-app-source-generated-chunk-npm-specifier-unresolved`：chunk 文件存在且经 IPC/staging
  进入 runtime candidate，但 chunk body 内仍有未被 no-localhost resolver 处理的 bare npm
  specifier。
- `DesktopTraceInspector` 的 cumulative gate 允许 v1.32 当前 app build-frontend evidence
  作为旧 generated/lazy blocker gate 的 successor evidence，避免新 trace 必须复现已被替换的
  历史 blocker。

固定 trace：

```bash
docs/traces/flow-ipc-v1_32.ndjson
```

已通过：

```bash
bash -n bin/vaadin-desktop
/usr/bin/swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools,vaadin-desktop-flow -am -DskipTests compile
bash bin/flow-client-resource-smoke
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32-build-frontend-output docs/traces/flow-ipc-v1_32.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32 docs/traces/flow-ipc-v1_32.ndjson
```

## 已更新

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
feat: add current app build-frontend output contract
```
