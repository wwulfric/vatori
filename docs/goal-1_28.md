# Goal 1.28: Generated Lazy Build Lifecycle Trace

状态：待执行

`docs/goal.md` 是长期愿景和完成度账本。v1.27 已完成
`flow-ipc-generated-lazy-real-build-probe-v1_27`：仓库内已经有一个普通 Flow
`@Route` + `@JavaScript(loadMode = LoadMode.LAZY)` 的真实 app/build 输入 probe，
trace 能证明 route/class/dependency 被观察到，真实
`generated-flow-imports-VW7hRaNw.js` 仍是 `Promise.resolve(0)` no-op table，剩余
blocker 收敛为
`scanner-output-not-connected-to-dev-bundle-generated-imports`。

v1.28 的目标是继续沿着这个 blocker 往下挖：把“scanner 输出没有连接到
dev-bundle/generated imports”拆成可审计的 Vaadin build lifecycle 证据，明确当前
缺口究竟发生在 scanner 输入、frontend build/update-imports 任务触发、generated imports
写入目标、dev-bundle artifact 选择，还是 no-localhost runtime 使用了 stale/static
dev-bundle 输出。

## 核心目标

新增 `flow-ipc-generated-lazy-build-lifecycle-v1_28` contract，覆盖：

- scanner lifecycle：v1.27 probe route、trigger class 和 lazy JavaScript dependency
  是否进入 Vaadin `FrontendDependencies` / route registry / lazy route chunk input。
- build lifecycle：当前 test app 的 frontend build、prepare frontend、update imports
  或 dev-bundle artifact resolution 是否运行，运行来源是什么。
- generated imports source：runtime 使用的
  `vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-*.js` 是来自静态
  packaged dev-bundle、当前 app build 输出，还是 copied/stale artifact。
- write target：如果 Vaadin build 生成或尝试生成 `generated-flow-imports`，trace 要记录
  source path、target path、mtime/sha256、table shape 和是否覆盖 runtime artifact。
- decision layer：如果仍没有真实 `./chunks/chunk-*.js`，failure layer 必须比 v1.27 更窄，
  例如 `lazy-route-not-in-route-registry`、
  `update-imports-not-run-for-ipc-runtime`、
  `generated-imports-written-outside-runtime-bundle`、
  `runtime-using-stale-packaged-dev-bundle` 或
  `vaadin-build-does-not-emit-chunk-for-loadmode-lazy-route-probe`。

v1.28 的完成条件不是必须真实生成 chunk file；完成条件是把 v1.27 的 build lifecycle
黑箱拆成可复现 trace 证据，并指出下一步应该连接哪个 Vaadin build/runtime 边界。如果真实
chunk 出现，则必须沿用 v1.22 executor path 做 IPC resolver / custom-scheme staging /
import / side-effect proof，并把 real-build parity 状态更新为 positive。

## 不做什么

- 不把 v1.22 repository-local fixture 当成真实 app/build 输出。
- 不把普通 `LoadMode.LAZY` runtime script 执行当成 generated
  `Flow.loadOnDemand(chunkId)` chunk-file parity。
- 不扩大到 production owner owned runtime、dev-mode/Vite 完整执行、Grid/Upload 或 durable
  push。
- 不把 compatibility mode 或 localhost dev-server 行为描述成最终 Flow IPC 架构。

## 实现要求

### Java / Vaadin build diagnostics

- 在 `vaadin-desktop-flow` 中为 v1.27 probe 增加 build lifecycle 诊断 helper，优先从
  Vaadin public/internal build metadata、classpath resources、dev-bundle paths 和本地
  upstream Vaadin source 结论中取证。
- 如果需要读取 filesystem artifact，记录 redacted path、mtime、byte count、sha256 和
  table shape；不要只记录布尔值。
- 对 scanner/build/runtime 三层分别输出 observed/source/status/failureLayer，避免一个
  宽泛 blocker 覆盖全部信息。

建议 metadata：

- `generatedLazyBuildLifecycleVersion=flow-ipc-generated-lazy-build-lifecycle-v1_28`
- `generatedLazyBuildLifecycleInputRoute=<route>`
- `generatedLazyBuildLifecycleInputRouteClass=<class>`
- `generatedLazyBuildLifecycleInputDependency=<context resource>`
- `generatedLazyBuildLifecycleScannerSource=<source>`
- `generatedLazyBuildLifecycleScannerStatus=<observed|missing|unknown>`
- `generatedLazyBuildLifecycleBuildTaskSource=<source>`
- `generatedLazyBuildLifecycleUpdateImportsObserved=<true|false>`
- `generatedLazyBuildLifecycleGeneratedImportsSource=<static-dev-bundle|current-build|copied|unknown>`
- `generatedLazyBuildLifecycleGeneratedImportsRuntimePath=<redacted path>`
- `generatedLazyBuildLifecycleGeneratedImportsSha256=<sha256>`
- `generatedLazyBuildLifecycleGeneratedImportsTableShape=<no-op-promise-resolve-0|branch-table-dynamic-import>`
- `generatedLazyBuildLifecycleWriteTarget=<redacted path>`
- `generatedLazyBuildLifecycleWriteTargetObserved=<true|false>`
- `generatedLazyBuildLifecycleChunkStatus=<not-generated|available|blocked>`
- `generatedLazyBuildLifecycleFailureLayer=<classification>`
- `generatedLazyBuildLifecycleNextResolutionStep=<next step>`

### Swift host / WebView diagnostics

- Swift host / WebView 要把 v1.28 metadata 传播到 `resourceRequest`、`resourceResponse`、
  `resourceStage` 和 `webview.dom`。
- DOM summary 必须同时展示 scanner/build/runtime 三层状态，避免只在 Java response 里有证据。
- 如果 runtime 使用的是 stale/static dev-bundle artifact，DOM summary 要明确记录 runtime
  artifact source 和当前 app probe 的 mismatch。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28-generated-lazy-build-lifecycle docs/traces/flow-ipc-v1_28.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28 docs/traces/flow-ipc-v1_28.ndjson
```

`--require-flow-ipc-v1_28-generated-lazy-build-lifecycle` 只验证 v1.28 delta；
`--require-flow-ipc-v1_28` 继承 v1.27 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28-generated-lazy-build-lifecycle .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_28.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28-generated-lazy-build-lifecycle docs/traces/flow-ipc-v1_28.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28 docs/traces/flow-ipc-v1_28.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27 docs/traces/flow-ipc-v1_27.ndjson
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
feat: add generated lazy build lifecycle trace
```
