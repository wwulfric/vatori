# Goal 1.26: Generated Lazy Source Boundary

状态：已完成

`docs/goal.md` 是长期愿景和完成度账本。v1.25 已完成 packaging-time staged
frontend baseline，但真实 app/build 的 generated lazy chunk file parity 仍未完成：
当前真实 `generated-flow-imports-*.js` 仍是 `Promise.resolve(0)` no-op table，v1.22
只证明 repository fixture 在存在 `./chunks/chunk-*.js` 分支时可执行。

v1.26 的目标是把这个 blocker 继续从“当前 build 没有 chunk file”推进为可审计的
**Vaadin generated lazy source boundary**：明确上游 Vaadin 生成 `./chunks/chunk-*.js`
的源机制、当前 smoke/dev-bundle 为什么没有产生真实 chunk，以及下一步必须走的
build/SPI 边界。

## 核心目标

新增 `flow-ipc-generated-lazy-source-boundary-v1_26` contract，覆盖：

- 上游 source mechanism：
  `com.vaadin.flow.server.frontend.AbstractUpdateImports` 只在 lazy route 的
  `lazyJavascript` 或 `lazyCss` 非空时生成 branch table dynamic import。
- scanner/input boundary：
  `FrontendDependencies`、`ChunkInfo(type=ROUTE,eager=false)`、`DependencyTrigger`
  和 lazy css/js 是真实 chunk-file 生成输入。
- current build proof：当前真实
  `vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js`
  仍是 `no-op-promise-resolve-0`，`chunkFileStatus=not-generated`。
- product decision：
  `build-spi-required-before-real-app-chunk-parity`，即下一步必须让 app build
  产生非空 lazy chunk 输入，或接入受支持 Vaadin lazy chunk SPI，才能把 v1.22
  fixture 替换为真实 app/build parity。

v1.26 的完成条件是：trace inspector 能证明 source mechanism、current-build no-op、
remaining blocker 和 next build/SPI decision 同时出现在 bootstrap manifest、
resource request/response、resourceStage 和 DOM summary 中。

## 不做什么

- 不声明真实 app/build generated chunk parity 已完成。
- 不把 v1.22 repository fixture 当成真实 Vaadin build 输出。
- 不引入内部 Vaadin API 作为稳定 public contract；`DependencyTrigger` 只作为当前
  upstream source boundary 证据。
- 不扩展到 production owner runtime、dev-mode/Vite 或更宽组件矩阵。

## 实现要求

### Java runtime metadata

`FlowClientResources` 的 component manifest 和真实 dev-bundle generated-flow-imports
resource response 要新增 v1.26 metadata：

- `generatedLazySourceBoundaryVersion=flow-ipc-generated-lazy-source-boundary-v1_26`
- `generatedLazySourceMechanism=vaadin-AbstractUpdateImports-generateLazyLoadedChunks`
- `generatedLazySourceArtifact=com.vaadin.flow.server.frontend.AbstractUpdateImports`
- `generatedLazySourceScanner=com.vaadin.flow.server.frontend.scanner.FrontendDependencies`
- `generatedLazySourceLazyRouteRule=ChunkInfo(type=ROUTE,eager=false)`
- `generatedLazySourcePositiveTableShape=branch-table-dynamic-import`
- `generatedLazySourceNoChunkFallback=Promise.resolve(0)`
- `generatedLazySourceCurrentBuildTableShape=no-op-promise-resolve-0`
- `generatedLazySourceCurrentBuildChunkStatus=not-generated`
- `generatedLazySourceCurrentBuildNoOpConfirmed=true`
- `generatedLazySourceDecision=build-spi-required-before-real-app-chunk-parity`
- `generatedLazySourceRemainingBlocker=real-app-generated-chunk-file-not-produced-by-current-build`

### Swift host / WebView diagnostics

Swift host / WebView 要把 v1.26 metadata 传播到：

- `resourceRequest`
- `resourceResponse`
- `resourceStage`
- `webview.dom`

DOM summary 必须能证明：

- v1.26 source boundary 被观察到。
- 当前真实 generated-flow-imports 仍是 no-op table。
- remaining blocker 没有被 v1.22 fixture 覆盖。
- 下一步决策指向 app build lazy chunk 生成或受支持 Vaadin lazy chunk SPI。

### Trace inspector

新增 gates：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26-generated-lazy-source-boundary docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26 docs/traces/flow-ipc-v1_26.ndjson
```

`--require-flow-ipc-v1_26-generated-lazy-source-boundary` 只验证 v1.26 delta；
`--require-flow-ipc-v1_26` 继承 v1.25 cumulative gate。

## 验收命令

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26-generated-lazy-source-boundary .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26 .build/vaadin-desktop-host-swift/host.out.log
```

固定 trace：

```bash
cp .build/vaadin-desktop-host-swift/host.out.log docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26-generated-lazy-source-boundary docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26 docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25 docs/traces/flow-ipc-v1_25.ndjson
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
feat: add generated lazy source boundary gate
```

## 完成记录

v1.26 已完成并固定 trace 为 `docs/traces/flow-ipc-v1_26.ndjson`。实现内容：

- Java runtime manifest / resource response 新增
  `flow-ipc-generated-lazy-source-boundary-v1_26` metadata。
- Swift host / WebView diagnostics 将 source boundary 传播到 `resourceRequest`、
  `resourceResponse`、`resourceStage` 和 `webview.dom`。
- `DesktopTraceInspector` 新增
  `--require-flow-ipc-v1_26-generated-lazy-source-boundary` delta gate 和
  `--require-flow-ipc-v1_26` cumulative gate。
- `flow-client-resource-smoke` 验证 bootstrap manifest 与 generated-flow-imports
  response 中的 v1.26 metadata。
- 真实 no-activate 桌面 smoke 通过，且 `webview.dom` 记录当前真实 generated-flow-imports
  仍为 no-op table / `chunk-file-not-generated` blocker。

通过的关键验收命令：

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -q -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -pl vaadin-desktop-devtools -am -DskipTests compile
bash bin/flow-client-resource-smoke
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26-generated-lazy-source-boundary docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26 docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25 docs/traces/flow-ipc-v1_25.ndjson
```
