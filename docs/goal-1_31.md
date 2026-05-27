# Goal 1.31: Vaadin Frontend Build Preflight Availability

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.30 已完成
`flow-ipc-current-app-frontend-build-output-v1_30`：`dev-ipc` 可以通过
`VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1` 运行可审计 Vaadin frontend build
preflight，并把 command/status/log/output inventory、generated imports candidates、
runtime source、chunk status、failure layer 和 next step 写入 manifest、resource
request/response/stage 与 DOM diagnostics。

v1.31 已把这层 blocker 推进到“build step 可执行，并且 output 被真实检查/接入”的层级：
显式 `VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1` 时，preflight 使用项目本地 Maven
repo 解析 `com.vaadin:vaadin-maven-plugin:25.1.5` 并成功执行 `prepare-frontend`；trace
记录 `pluginResolutionStatus=resolved`、`goalResult=success`、`outputUsability=
prepare-frontend-generated-scaffold-no-generated-imports` 和 failure layer
`frontend-build-plugin-resolved-output-incomplete`。当前 Vaadin 输出仍未产生当前 app 的
`generated-flow-imports*.js` 或 `chunks/chunk-*.js`，所以这不是 real generated chunk parity。

## 核心目标

新增 `flow-ipc-frontend-build-preflight-availability-v1_31` contract，覆盖：

- Maven plugin availability：让 `dev-ipc` frontend build preflight 能明确区分 offline
  missing plugin、online/populated local-repo、plugin resolved but goal failed、plugin
  succeeded but output incomplete、plugin succeeded with usable generated imports/chunks。
- Local repo discipline：所有下载/解析仍使用项目本地 Maven repo
  `/Users/bytedance/IdeaProjects/vatori/.m2/repository`，不写用户全局 Maven cache。
- Runtime source advancement：一旦当前 app build output 产生 usable generated imports，
  generated/lazy resolver 必须优先使用该 output；只有 output 明确缺失目标 artifact 时才保留
  packaged dev-bundle fallback。
- Trace classification：把 v1.30 的 `build-step-unavailable` 收敛为更窄层级，例如
  `frontend-build-plugin-resolved-output-incomplete`、
  `frontend-build-goal-failed-before-output`、
  `current-app-build-output-has-no-generated-lazy-chunk` 或
  `current-app-build-output-generated-chunk-executed`。

Acceptable outcomes:

- Preferred：frontend build preflight 成功，当前 app build output 产生真实 generated imports
  和 `./chunks/chunk-*.js`，runtime 使用当前 app output，经 no-localhost resolver/staging
  执行真实 generated lazy chunk，并记录 callback + side-effect proof。
- Acceptable：frontend build preflight 成功或至少 plugin resolved，但当前 Vaadin 输出仍不产生
  generated lazy chunk；trace 必须记录 plugin resolution、build result、output roots、
  generated imports metadata、table shape、chunk status 和更窄 failure layer。
- Acceptable：plugin/goal 可解析但因 Node/npm、route scanner、Vaadin plugin config、
  production mode 或内部 SPI 失败；trace 必须记录真实 failure layer 和下一步，不能继续停在
  `build-step-unavailable`。

## 不做什么

- 不把下载到项目本地 repo 的 packaged Vaadin dev-bundle jar 伪装成当前 app build output。
- 不把 v1.22 repository fixture 当成当前 app generated chunk parity。
- 不默认写用户全局 Maven cache。
- 不扩大到 production owner side-effect success、dev-mode/Vite 完整执行、Grid/Upload 或 durable push。
- 不使用 localhost compatibility mode 作为 Flow IPC 成功证据。

## 实现要求

### CLI / build preflight

- 为 `bin/vaadin-desktop dev-ipc` 的 frontend build preflight 增加明确的 availability mode。
  可以使用环境变量，例如：
  - `VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1`
  - `VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1`
- offline 默认行为必须保持可重复；online/populate 行为必须显式开启。
- build command 必须继续使用项目本地 Maven repo。
- trace/log metadata 必须区分 plugin missing、plugin resolved、goal missing、goal failed、
  build succeeded、output incomplete。

### Java resolver / metadata

- 在 `FlowClientResources` 上新增 v1.31 metadata，保留 v1.30 字段并增加：
  - plugin resolution status
  - build availability mode
  - effective Maven mode
  - frontend build goal result
  - current app output usability
  - generated imports runtime source decision
- 当前 app output 可用时，resource body/path/sha/table shape 必须来自当前 app output。

### Swift host / WebView diagnostics

- 将 v1.31 metadata 传播到 `resourceRequest`、`resourceResponse`、`resourceStage`、
  `resourceLoad` 和 `webview.dom`。
- DOM summary 必须显示 build preflight 是否越过 v1.30 unavailable 层，以及 runtime 是否仍在
  packaged dev-bundle fallback。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31-frontend-build-preflight-availability docs/traces/flow-ipc-v1_31.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31 docs/traces/flow-ipc-v1_31.ndjson
```

`--require-flow-ipc-v1_31-frontend-build-preflight-availability` 只验证 v1.31 delta；
`--require-flow-ipc-v1_31` 继承 v1.30 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
VAADIN_DESKTOP_MAVEN_OFFLINE=0 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31-frontend-build-preflight-availability .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_31.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31-frontend-build-preflight-availability docs/traces/flow-ipc-v1_31.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31 docs/traces/flow-ipc-v1_31.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30 docs/traces/flow-ipc-v1_30.ndjson
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
docs: add goal 1.31 frontend build preflight availability
```

## 完成记录

- CLI 新增 v1.31 availability metadata：availability mode、effective Maven mode、plugin
  coordinates/resolution、goal result 和 output usability；默认 preflight 保持 offline，
  online local-repo populate 必须显式设置
  `VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_ONLINE=1`。
- Java/Swift trace 贯穿
  `flow-ipc-frontend-build-preflight-availability-v1_31` 到 manifest、resource
  request/response/stage 和 DOM diagnostics。
- Inspector 新增 `--require-flow-ipc-v1_31-frontend-build-preflight-availability` 和
  `--require-flow-ipc-v1_31` gates。
- 固定 trace：`docs/traces/flow-ipc-v1_31.ndjson`。
- 验收结论：插件已解析且 `prepare-frontend` 成功，但输出仍是 scaffold/no generated imports；
  下一步应进入 full frontend build/package 或 Vaadin lazy chunk SPI，而不是继续处理 Maven plugin
  availability。
