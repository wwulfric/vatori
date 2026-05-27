# Goal 1.27: Generated Lazy Real Build Probe

状态：待执行

`docs/goal.md` 是长期愿景和完成度账本。v1.26 已完成
`flow-ipc-generated-lazy-source-boundary-v1_26`：它证明 Vaadin 只有在
`AbstractUpdateImports` 收到非空 `lazyJavascript` / `lazyCss` 的 lazy route
`ChunkInfo` 时才会生成真实 `./chunks/chunk-*.js` branch table，并确认当前
smoke dev-bundle 的真实 `generated-flow-imports-*.js` 仍是 `Promise.resolve(0)`
no-op table。

v1.27 的目标是把 v1.26 的 source boundary 推进为一个真实 app/build probe：
让仓库内出现一个可审计的 non-eager route / lazy dependency build input，并用
trace gate 判断该输入是否进入当前 Vaadin generated-flow-imports 输出。如果当前
构建仍未生成真实 chunk file，v1.27 必须把 blocker 精确收敛到 build lifecycle /
scanner / generated imports output 的哪一层，而不是继续停留在“当前没有 chunk”。

## 核心目标

新增 `flow-ipc-generated-lazy-real-build-probe-v1_27` contract，覆盖：

- build input：一个真实 Flow route 或 app classpath probe，能表达
  `ChunkInfo(type=ROUTE,eager=false)` + `DependencyTrigger` + lazy JavaScript/CSS 输入。
- scanner evidence：记录 `FrontendDependencies` 是否观察到该 route、trigger class、
  `lazyJavascript` / `lazyCss` candidate 和 eager/non-eager classification。
- generated imports evidence：解析当前真实
  `vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-*.js`，判断它是否从
  no-op table 变成 branch table dynamic import。
- runtime decision：如果真实 chunk file 出现，则沿用 v1.22 executor path 进入
  IPC resolver / staging / import / side-effect proof；如果没有出现，则记录更窄的
  failure layer，例如 `lazy-route-input-not-in-generated-imports`、
  `frontend-build-not-run-for-lazy-route-input` 或
  `scanner-output-not-connected-to-dev-bundle-generated-imports`。

v1.27 的完成条件不是必须真实生成 chunk file；完成条件是有一个真实 app/build 输入
probe，并且 trace inspector 能证明该输入在 scanner / generated imports / runtime
resolver 三层中的实际落点。

## 不做什么

- 不把 repository-local v1.22 fixture 重新包装成真实 app/build parity。
- 不声明 generated lazy parity 已完成，除非当前 app/build 真的产生并执行真实
  `./chunks/chunk-*.js` artifact。
- 不把普通 `@JavaScript(loadMode = LoadMode.LAZY)` 的 runtime path 当成 generated
  `Flow.loadOnDemand(chunkId)` chunk-file parity。
- 不扩大到 production owner、dev-mode/Vite、Grid/Upload 或 durable push。

## 实现要求

### App/build input probe

- 在 smoke/framework app 或一个新的 focused validation app 中加入 non-eager route /
  lazy dependency 输入，保持普通 Flow Java API 形态。
- 如果需要新增 frontend resource，必须让它像外部项目资源一样被 app build /
  classpath 发现，而不是 hard-code 到 host 里。
- 记录 route name、trigger class、dependency path、eager flag、expected chunk id /
  chunk artifact candidate。

### Java runtime metadata

`FlowClientResources` 或新的 build-probe helper 要新增 v1.27 metadata：

- `generatedLazyRealBuildProbeVersion=flow-ipc-generated-lazy-real-build-probe-v1_27`
- `generatedLazyRealBuildInputRoute=<route>`
- `generatedLazyRealBuildInputTrigger=<class>`
- `generatedLazyRealBuildInputEager=false`
- `generatedLazyRealBuildScannerObserved=<true|false>`
- `generatedLazyRealBuildLazyJavascriptObserved=<true|false>`
- `generatedLazyRealBuildGeneratedImportsTableShape=<no-op-promise-resolve-0|branch-table-dynamic-import>`
- `generatedLazyRealBuildChunkStatus=<not-generated|available|blocked>`
- `generatedLazyRealBuildFailureLayer=<classification>`

### Swift host / WebView diagnostics

Swift host / WebView 要把 v1.27 metadata 传播到：

- `resourceRequest`
- `resourceResponse`
- `resourceStage`
- `webview.dom`

如果真实 chunk file 仍未出现，DOM summary 必须能证明：

- v1.27 build input probe 被观察到。
- 当前真实 generated-flow-imports 输出是否包含 branch table。
- failure layer 比 v1.26 更窄。
- v1.22 fixture 没有被当成真实 app/build 输出。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27-generated-lazy-real-build-probe docs/traces/flow-ipc-v1_27.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27 docs/traces/flow-ipc-v1_27.ndjson
```

`--require-flow-ipc-v1_27-generated-lazy-real-build-probe` 只验证 v1.27 delta；
`--require-flow-ipc-v1_27` 继承 v1.26 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27-generated-lazy-real-build-probe .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_27.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27-generated-lazy-real-build-probe docs/traces/flow-ipc-v1_27.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27 docs/traces/flow-ipc-v1_27.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26 docs/traces/flow-ipc-v1_26.ndjson
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
feat: add generated lazy real build probe
```
