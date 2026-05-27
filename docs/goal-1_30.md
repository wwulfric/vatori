# Goal 1.30: Current App Frontend Build Output Materialization

状态：已完成

完成记录：

- `bin/vaadin-desktop dev-ipc` 增加 opt-in
  `VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1` frontend build preflight，使用项目本地
  Maven repo，记录 redacted command、exit/status、duration、summary 和 stdout/stderr
  log path。
- `FlowClientResources` 新增
  `flow-ipc-current-app-frontend-build-output-v1_30` metadata，记录当前 app output
  inventory、generated imports candidates、runtime source、chunk status、failure layer
  和 next step。
- Swift host / WebView 将 v1.30 metadata 传播到 resource request/response/stage 和
  DOM diagnostics。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_30-current-app-frontend-build-output` 与
  `--require-flow-ipc-v1_30` gates。
- 本机真实窗口验收通过 v1.30 gates。当前离线环境缺少
  `com.vaadin:vaadin-maven-plugin:25.1.5`，所以 accepted outcome 是
  `currentAppFrontendBuildOutputRuntimeSource=build-step-unavailable` 和
  `currentAppFrontendBuildFailureLayer=vaadin-frontend-build-step-unavailable`，不是
  真实 generated lazy chunk parity。

`docs/goal.md` 是长期愿景和完成度账本。v1.29 已完成
`flow-ipc-generated-lazy-app-build-output-v1_29`：trace 现在证明 no-localhost
`dev-ipc` runtime 会查找当前 app build output，但当前 smoke app build output 中没有可用
generated imports / generated lazy chunk，runtime 只能明确 fallback 到 packaged Vaadin
dev-bundle，并把 failure layer 收敛为 `app-build-output-missing-generated-imports`。

v1.30 的目标是把这个 blocker 从“发现当前 app output 缺失”推进到“有一条可执行、可追踪、
可回归的当前 app frontend build output materialization 路径”。这一步不要求强行证明 Vaadin
一定会为当前 probe 生成真实 chunk；但必须让 runtime 不再停在“没有运行/接入 app build output”
这一层。

## 核心目标

新增 `flow-ipc-current-app-frontend-build-output-v1_30` contract，覆盖：

- Build-step invocation：为 `dev-ipc` smoke app 和 framework app 增加可审计的当前 app
  frontend build/output materialization 路径，优先使用 Vaadin Maven plugin
  `prepare-frontend` / `build-frontend` 或等价受支持 build step。
- Build output inventory：记录 build step 是否运行、运行命令、模块、输出根、generated
  imports 文件、frontend generated files、`target` / `frontend/generated` / `VAADIN/build`
  候选以及 redacted path、mtime、byte count、sha256。
- Runtime source switch：generated/lazy resolver 必须优先使用当前 app materialized output。
  只有当前 app output 被证明缺少目标 artifact 时，才可以 fallback 到 packaged dev-bundle。
- Parity classification：如果当前 app output 产生真实 chunk table 和 `./chunks/chunk-*.js`，
  必须沿用 v1.22 executor path 执行并记录 callback/side-effect proof；如果仍未产生 chunk，
  failure layer 必须收敛到当前 app output 层，例如
  `current-app-build-output-has-no-generated-lazy-chunk`，不能回退成
  `app-build-output-missing-generated-imports`。

Acceptable outcomes:

- Preferred：`dev-ipc` 启动前或启动中运行/接入当前 app frontend build step，runtime 使用当前
  app output 中的 generated imports 和 chunk file，通过 IPC resolver / custom-scheme staging
  执行真实 generated lazy chunk，并记录 callback + side-effect proof。
- Acceptable：当前 app build output 被 materialize 并由 runtime 使用，但 Vaadin 当前输出仍是
  no-op table 或没有 chunk file；trace 记录 `current-app-build-output` runtime source、
  generated imports metadata、table shape、chunk status 和更窄 failure layer
  `current-app-build-output-has-no-generated-lazy-chunk` 或同等精确层。
- Acceptable：Vaadin build step 需要 Node/npm、production mode、plugin config、route scanner
  或内部 SPI 才能产生目标 output；trace 必须记录实际 build command、exit status、stdout/stderr
  summary、输出目录、缺失 artifact、下一步和 failure layer，例如
  `vaadin-frontend-build-step-output-incomplete`。不能只记录“missing build output”。

## 不做什么

- 不把 packaged Vaadin dev-bundle jar artifact 当作当前 app build output。
- 不把 v1.22 repository-local fixture 当作当前 app/build output。
- 不把普通 `LoadMode.LAZY` script 执行当作 generated `Flow.loadOnDemand(chunkId)` chunk-file parity。
- 不扩大到 production owner side-effect success、dev-mode/Vite 完整执行、Grid/Upload 或 durable push。
- 不使用 localhost compatibility mode 作为 Flow IPC 成功证据。
- 不要求修改本地 upstream Vaadin checkout；如需参考，只读使用。

## 实现要求

### CLI / build-step integration

- 在 `bin/vaadin-desktop dev-ipc` 或可明确开启的 build-preflight 路径中，为目标 module 增加
  当前 app frontend build output materialization。
- 如果默认启用风险过大，可以先用显式环境变量或 CLI flag，例如
  `VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1`，但真实窗口验收必须覆盖该路径。
- build step 必须使用项目本地 Maven repo：
  `/Users/bytedance/IdeaProjects/vatori/.m2/repository`。
- 对 build step 运行结果记录 redacted command、module、exit code、duration、output roots、
  generated imports candidates 和错误摘要。

### Java resolver / metadata

- `FlowClientResources` 的 generated/lazy metadata 必须区分：
  - `current-app-build-output`
  - `current-app-build-output-incomplete`
  - `current-app-build-output-no-generated-lazy-chunk`
  - `packaged-dev-bundle-fallback`
  - `build-step-unavailable`
- 对每个候选记录 redacted path、exists、protocol、mtime、byte count、sha256、table shape、
  chunk references 和 source classification。
- 当前 app output 存在时，runtime 必须优先使用它，而不是继续读 packaged dev-bundle。

建议 metadata：

- `currentAppFrontendBuildOutputVersion=flow-ipc-current-app-frontend-build-output-v1_30`
- `currentAppFrontendBuildStepCommand=<redacted command>`
- `currentAppFrontendBuildStepExitCode=<code>`
- `currentAppFrontendBuildStepStatus=<success|failed|skipped|unavailable>`
- `currentAppFrontendBuildOutputRuntimeSource=<current-app-build-output|current-app-build-output-incomplete|packaged-dev-bundle-fallback>`
- `currentAppFrontendBuildOutputRoot=<redacted path>`
- `currentAppFrontendBuildGeneratedImportsPath=<redacted path>`
- `currentAppFrontendBuildGeneratedImportsObserved=<true|false>`
- `currentAppFrontendBuildGeneratedImportsSha256=<sha256>`
- `currentAppFrontendBuildGeneratedImportsTableShape=<no-op-promise-resolve-0|branch-table-dynamic-import|missing|unrecognized>`
- `currentAppFrontendBuildChunkReferences=<count/list>`
- `currentAppFrontendBuildChunkStatus=<available|not-generated|missing-output|blocked>`
- `currentAppFrontendBuildFailureLayer=<classification>`
- `currentAppFrontendBuildNextResolutionStep=<next step>`

### Swift host / WebView diagnostics

- Swift host / WebView 要把 v1.30 metadata 传播到 `resourceRequest`、`resourceResponse`、
  `resourceStage`、`resourceLoad` 和 `webview.dom`。
- DOM summary 必须展示 runtime source 是否已经离开 packaged dev-bundle fallback。
- 如果真实 chunk 可用，必须通过同一 no-localhost resolver/staging path 执行，并记录
  callback / side-effect proof。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30-current-app-frontend-build-output docs/traces/flow-ipc-v1_30.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30 docs/traces/flow-ipc-v1_30.ndjson
```

`--require-flow-ipc-v1_30-current-app-frontend-build-output` 只验证 v1.30 delta；
`--require-flow-ipc-v1_30` 继承 v1.29 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30-current-app-frontend-build-output .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_30.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30-current-app-frontend-build-output docs/traces/flow-ipc-v1_30.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30 docs/traces/flow-ipc-v1_30.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29 docs/traces/flow-ipc-v1_29.ndjson
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
feat: materialize current app frontend build output
```
