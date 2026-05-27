# Goal 1.22: Generated Lazy Chunk File Positive Execution Boundary

状态：已完成。

完成证据：

- 新增 classpath fixture `META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js`
  和 `META-INF/vatori/generated/flow/chunks/chunk-v1_22.js`。
- Java resource metadata、Swift host execution probe、`DesktopTraceInspector` delta/cumulative
  gate 和 `flow-client-resource-smoke` assertions 已落地。
- `docs/traces/flow-ipc-v1_22.ndjson` 收录真实 no-activate smoke trace。
- 验证通过：
  - `git diff --check`
  - `bash -n bin/vaadin-desktop`
  - `swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift`
  - `mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile`
  - `bash bin/flow-client-resource-smoke`
  - `bash bin/uidl-adapter-smoke`
  - `bash bin/uidl-rpc-smoke`
  - `bash bin/flow-ipc-scope-smoke`
  - `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22-generated-chunk-fixture docs/traces/flow-ipc-v1_22.ndjson`
  - `bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22 docs/traces/flow-ipc-v1_22.ndjson`
  - no-activate real-window smoke app + `--require-flow-ipc-v1_22`
  - no-activate framework validation app + `--require-flow-ipc-v1_3-framework`

结论：v1.22 证明了 generated `Flow.loadOnDemand` chunk-file table shape 存在时，当前
no-localhost resolver、parser、staging/import、callback 和 side-effect proof 可以闭环。
它不表示当前 Vaadin app/build 已真实生成 lazy chunk；v1.12 的真实 dev-bundle no-op table /
`chunk-file-not-generated` blocker 仍保留为后续工作。

`docs/goal.md` 是长期愿景和完成度账本。v1.21 已经把 flow-client、component
frontend、theme/static、generated/lazy、Flow lazy dependency、production owner 和
dev-mode package graph 收束到统一 `frontendResolverContract`。但 generated/lazy path
仍只有两类证据：

- 真实 Vaadin dev-bundle `generated-flow-imports-*.js` 会执行，但当前 app/build 输出是
  `Promise.resolve(0)` no-op table，没有真实 `./chunks/chunk-*.js`。
- 普通 Flow `LoadMode.LAZY` dependency 已经能经 visible Flow client / DependencyLoader
  通过 IPC 触发、解析、stage 和执行，但这不是 generated `Flow.loadOnDemand(chunkId)`
  chunk-file path。

**核心目标：为 generated `Flow.loadOnDemand` chunk-file path 增加一个 Vaadin 同形状的正向
chunk-file execution fixture，证明当前 no-localhost resolver、parser、staging/import、
callback 和 side-effect proof 在真实 chunk file 存在时可以闭环；同时继续明确真实 dev-bundle
当前仍是 `chunk-file-not-generated` blocker。**

## 为什么选择这个目标

v1.12 已经证明当前真实 dev-bundle 没有生成 chunk file。继续只记录 no-op blocker，无法回答
另一个关键问题：一旦 Vaadin build 真的产出

```js
pending.push(import('./chunks/chunk-*.js'))
```

当前 Flow IPC frontend/resource pipeline 是否已经有能力解析、请求、stage、import 并验证
side effect。

v1.22 不把 fixture 包装成“当前 app/build 真实生成了 lazy chunk”。它要补齐的是正向执行边界：
使用 Vaadin `AbstractUpdateImports` 同形状的 generated-flow-imports fixture 和 classpath chunk
file，走同一个 IPC resource resolver 和 WebView staging path，产生可被 inspector gate 验证的
`flow-ipc-loadondemand-chunk-file-execution-v1_22` 证据。

这会让下一步更清楚：

- 如果后续 app/build 能产生真实 `./chunks/chunk-*.js`，resolver 已有正向执行路径。
- 如果 Vaadin 当前版本或 build 模式不产生 generated chunk，则可以把普通 `LoadMode.LAZY` 或
  受支持 lazy SPI 作为替代路径单独评估，而不是把 no-op table 当成技术失败。

## 继承边界

以下能力在 v1.22 中视为基线，不能回退：

- v1.21 `frontendResolverContract` 继续通过，并覆盖 generated/lazy family。
- v1.12 真实 dev-bundle no-op table proof 继续通过；`chunk-file-not-generated` 不能被隐藏。
- v1.11 visible Flow client 触发 `Flow.loadOnDemand(chunkId)` 的证据继续通过。
- v1.10 ordinary Flow `LoadMode.LAZY` dependency execution 继续通过。
- v1.20 production owner-owned runtime fallback 继续明确为 accepted blocker。
- BigDecimalField、TextField、Checkbox、Select、framework validation、push ordering、
  resync、capability 和 file transfer 不退化。

## 非目标

- 不要求当前 Vaadin dev-bundle 真的生成 `./chunks/chunk-*.js`。
- 不把 repository fixture 当成当前 app/build production generated chunk success。
- 不要求完成 production owner side-effect success。
- 不要求完成 dev-mode/Vite execution。
- 不扩大组件覆盖到 Grid/VirtualList、ComboBox、DatePicker、Dialog 或 Upload。
- 不使用 localhost HTTP、`file://` 直读或 compatibility mode 作为 v1.22 成功证据。

## 实现范围

### 1. Vaadin-Shaped Positive Fixture

新增 classpath fixture：

- `META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js`
- `META-INF/vatori/generated/flow/chunks/chunk-v1_22.js`

fixture 必须采用 Vaadin `AbstractUpdateImports` 当前同形状的 table：

```js
const loadOnDemand = (key) => {
  const pending = [];
  if (key === '<fixture chunk id>') {
    pending.push(import('./chunks/chunk-v1_22.js'));
  }
  return Promise.all(pending);
}
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
```

chunk body 必须产生可验证 side effect，例如：

- `window.VaadinDesktopGeneratedLazyChunkV122.executed=true`
- chunk id、source artifact、execution version 和 timestamp
- 可选 DOM/window event，便于 WebView 诊断

### 2. Java Resource Metadata

Java manifest / resource response 增加 v1.22 metadata：

- `generatedLazyChunkFileExecutionVersion=flow-ipc-loadondemand-chunk-file-execution-v1_22`
- `generatedLazyChunkFileFixtureSource=repository-vaadin-shaped-generated-flow-imports-v1_22`
- `generatedLazyChunkFileFixtureImportsArtifact=...generated-flow-imports-v1_22.js`
- `generatedLazyChunkFileFixtureChunkArtifact=...chunks/chunk-v1_22.js`
- `generatedLazyChunkFileFixtureChunkId=<fixture chunk id>`
- parser 识别 fixture table shape 为 `branch-table-dynamic-import`
- `chunkFileStatus=available`
- `chunkFileBytes` / `chunkFileSha256`
- `loadOnDemandChunkFileSideEffectProof=chunk-file-body-available-for-staged-import`

验收要求：

- `flow-client-resource-smoke` 断言 v1.22 fixture imports 和 chunk response 都带 metadata。
- v1.12 no-op table metadata 继续保留，不被 v1.22 覆盖成 success。

### 3. Host Execution Path

Swift host / WebView runtime 增加一个受控 v1.22 execution probe：

- 通过 IPC 请求 fixture generated-flow-imports 和 chunk file body。
- 使用统一 resolver contract stage fixture imports 和 chunk。
- 将 `./chunks/chunk-v1_22.js` relative import 重写到 staged URL。
- 调用 fixture `window.Vaadin.Flow.loadOnDemand(<fixture chunk id>)`。
- 验证 side effect。
- 记录 `resourceStage` 和 `webview.dom` 证据：
  - `generatedLazyChunkFileExecutionVersion=flow-ipc-loadondemand-chunk-file-execution-v1_22`
  - `generatedLazyChunkFileFixtureObserved=true`
  - `generatedLazyChunkFileFixtureStageObserved=true`
  - `generatedLazyChunkFileFixtureExecuted=true`
  - `generatedLazyChunkFileFixtureSideEffectSatisfied=true`
  - imports artifact、chunk artifact、chunk id、sha256、stage URL policy、no-localhost proof

验收要求：

- v1.22 fixture execution 不能改变 v1.11/v1.12 对真实 dev-bundle no-op blocker 的判定。
- 如果 fixture chunk import 失败，trace 必须记录 browser/native error 和 failure layer。

### 4. Inspector Gate

新增两类 gate：

- `--require-flow-ipc-v1_22-generated-chunk-fixture`：只验证 v1.22 正向 fixture execution。
- `--require-flow-ipc-v1_22`：累计阶段完成 gate，表示 v1.21 baseline + v1.22 delta。

规则：

- 必须看到 v1.22 manifest / resource response / resourceStage / DOM evidence。
- 必须看到 generated-flow-imports fixture table 解析出可用 chunk file。
- 必须看到 chunk file staged/imported/executed 和 side-effect proof。
- 必须同时看到 v1.12 真实 dev-bundle no-op / `chunk-file-not-generated` 仍然存在。

交付：

- 新增 `docs/traces/flow-ipc-v1_22.ndjson`。
- 更新 `DesktopTraceInspector` 支持 v1.22 delta/cumulative gate。
- 保持 v1-v1.21 fixture gates 继续可运行。
- 保持 `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。

### 5. Documentation

更新：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/goal.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/acceptance-tools.md`
- `test-apps/flow-over-ipc-smoke-app/README.md`

文档必须明确：

- v1.22 是 generated chunk-file positive execution fixture，不是当前 app/build 真实 chunk parity。
- v1.12 no-op table / `chunk-file-not-generated` 仍是当前真实 dev-bundle blocker。
- v1.22 的价值是证明 resolver/executor 能处理真实 chunk-file table shape。
- 下一步可以选择让当前 app/build 产生真实 generated chunk、接入受支持 Vaadin lazy chunk SPI、
  或转向 production owner/dev-mode execution。

## 验收命令

基础检查：

```bash
git diff --check
bash -n bin/vaadin-desktop
swiftc -parse vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

Server-side smoke：

```bash
bash bin/flow-client-resource-smoke
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
```

Trace fixture：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21 docs/traces/flow-ipc-v1_21.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22-generated-chunk-fixture docs/traces/flow-ipc-v1_22.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22 docs/traces/flow-ipc-v1_22.ndjson
```

真实窗口 smoke：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22 .build/vaadin-desktop-host-swift/host.out.log
```

Framework validation：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
jps -l
```

## 完成判定

v1.22 完成时必须满足：

1. v1.22 fixture generated-flow-imports table 被 parser 识别为 `branch-table-dynamic-import`。
2. fixture chunk file 通过 IPC resource resolver 返回真实 body、sha256、byte count。
3. WebView stage/import fixture imports 和 chunk file，`Flow.loadOnDemand(<fixture chunk id>)`
   触发 chunk import，side effect 满足。
4. v1.12 真实 dev-bundle no-op table blocker 继续通过，不被 v1.22 fixture 覆盖。
5. v1.22 trace gates、真实窗口 smoke、framework validation 和基础编译检查全部通过。
