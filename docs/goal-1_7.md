# Goal 1.7: TextField Production Chunk Side-Effect Split

`docs/goal.md` 是长期愿景和完成度账本，不是已完成的阶段目标。v1.6 已完成的是
一个阶段切片：它没有宣称真实 `TextField` 已完整运行，而是把 TextField/npm graph
从 v1.5 的宽泛 staged blocker 收敛到一个可复现的具体 blocker：

- entry artifact：`META-INF/vatori/dev-bundle-entry/vaadin-text-field.js`
- transitive artifact：`vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js`
- staging strategy：Blob module rewrite
- failure layer：`custom-element-registry-conflict-after-production-indexhtml-reimport`
- browser error：`Cannot define multiple custom elements with the same tag name`
- duplicate tag：`vaadin-text-field`

本阶段目标是承接这个 exact blocker，处理 production `indexhtml` chunk 的 side effect，
让真实 TextField/npm graph 从 blocker 证据进入可执行路径；如果仍无法完整执行，
也必须证明 blocker 已越过 v1.6 的 duplicate custom element definition 层，收敛到
新的、更具体的 Vaadin/WebKit/runtime 层。

**核心目标：移除 v1.6 duplicate custom element blocker，使真实 TextField frontend
artifact graph 通过 no-localhost staged graph 执行，并验证真实 Flow `TextField`
component event path；旧 `@vaadin/text-field` shim 不能作为成功证据。**

## 继承边界

### 已完成的 v1.6 能力

以下能力在 v1.7 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app`
  继续启动 no-localhost Flow IPC smoke runtime。
- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app`
  继续启动普通开发者项目形态的 framework validation app。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell，不通过 localhost
  HTTP URL 加载核心 UI path。
- Visible Flow client engine 通过 generic viewport promotion 负责可见 renderer。
- v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6 fixture gates 和
  `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。
- Flow client classpath、component frontend、npm/frontend、theme/static、production bundle、
  dev-mode bundle 和 generated/lazy artifact families 都能通过 manifest 表达和验证。
- BigDecimalField、Checkbox、Select、Button、list/repeated update、desktop capability、
  file transfer、push ordering、resync 和 WebView error-reporting baseline 继续可验证。
- v1.5 staging resolver 能记录 `resourceStage`，并在 `webview.dom` 中区分 staged、
  executed、failed 和 strict blocker resource。
- v1.6 TextField graph metadata 能记录 `blockerVersion=flow-ipc-textfield-v1_6`、
  `textfieldGraphParityVersion=flow-ipc-textfield-graph-v1_6`、transitive artifacts、
  custom element registry before/after、failure layer、browser error 和 next step。
- 旧 `@vaadin/text-field` shim 只允许作为旧路径回归保护，不能作为 v1.7 成功证据。

### 本阶段关键约束

- v1.7 验收路径不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 不能恢复 Servlet/HTTP/WebSocket compatibility transport 来满足验收。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC 或 host-specific JS。
- 不能用空实现、伪 `TextField` class、fake custom element 或 WebView 专用 inline shim
  绕过真实 Vaadin artifact graph。
- 不能把 v1.6 的 duplicate custom element blocker 原样搬运为 v1.7 完成证据。
- 如果保留任何 shim 文件，必须在 trace、inspector 和文档里标记为 legacy fallback；
  v1.7 strict gate 不能把 shim path 计入成功。
- 不能把 resolver 写死为 smoke app DOM id；framework app 仍必须验证 generic viewport、
  ordinary Flow Java API 和 manifest-gated capability path。

## 实现范围

### 1. Production `indexhtml` Side-Effect Audit

目标：

- 审计当前 TextField graph 为什么通过 production `indexhtml-BfYYZi8S.js` 导入真实
  `TextField` class 时会重复注册 `vaadin-text-field`。
- 基于仓库内 dev-bundle artifact 和本地 Vaadin 源码树定位：
  - `c7 as TextField` export 的真实来源。
  - `defineCustomElement` helper 的来源和调用语义。
  - production `indexhtml` chunk 中哪些 top-level side effects 会注册 custom element。
  - BigDecimalField 路径首次定义 `vaadin-text-field` 的 artifact。
  - TextField direct probe 第二次定义 `vaadin-text-field` 的 artifact。
  - 是否存在 side-effect-free export、class-only entry 或可抽取的 focused module 边界。
- 对 TextField graph 增加 v1.7 metadata：
  - `textfieldSideEffectVersion=flow-ipc-textfield-side-effect-v1_7`
  - entry artifact
  - class export artifact
  - first definition source
  - second definition source
  - duplicate tag
  - chosen split/dedupe strategy
  - remaining blocker classification。

验收证据：

- `FlowClientResourceSmokeMain` 或等价 smoke 能检查 TextField v1.7 side-effect metadata。
- Trace 中的 `resourceRequest` / `resourceResponse` 能说明 TextField class export、
  production chunk dependency 和 registry side-effect chain。
- 文档能说明 v1.6 blocker 的具体成因，以及 v1.7 选择的拆解策略。

### 2. Side-Effect Split Or Registry Dedupe Strategy

目标：

在不新增伪组件、不恢复 localhost 的前提下，实现以下方向之一：

1. **Side-effect-free focused entry**
   - 从真实 Vaadin production/dev bundle 中抽取或重组 TextField class dependency，
     让 `TextField` class 可被导入而不重复执行 production app chunk 的 custom element
     registration side effects。
   - 抽取结果必须保留真实 Vaadin class 行为，不能替换为空实现。
2. **Custom scheme / file-backed module resolver**
   - 用 custom scheme、file-backed staged modules 或等价 resolver 保留 relative imports，
     并支持 deterministic module identity，避免同一 production chunk 被作为不同 Blob
     module 重复执行。
   - 需要明确 cache、cleanup、integrity 和 module identity 策略。
3. **Registry-aware production chunk dedupe**
   - 如果必须再次执行 production chunk，resolver 要能记录并避免重复注册同一 custom
     element，同时不隐藏真实 class/export 缺失。
   - 任何 registry guard 都必须 trace first/second definition source 和 skip reason。

验收证据：

- `resourceStage` 记录 v1.7 resolver version、source artifact、staged URL/scheme、
  module identity、cache hit/miss、cleanup policy、execution status 和 split/dedupe strategy。
- v1.7 trace 不再把 `Cannot define multiple custom elements with the same tag name`
  作为 TextField graph 的最终 blocker。
- 若成功，`webview.dom` 记录真实 TextField graph executed，例如
  `frontendArtifactTextFieldExecuted=true`、`frontendArtifactTextFieldExecutionVersion=flow-ipc-textfield-v1_7`。
- 若失败，`resourceStage` 记录 `blockerVersion=flow-ipc-textfield-v1_7`，且
  `failureLayer` 必须不同于 v1.6 的 duplicate custom element layer。

### 3. Real Flow `TextField` Component Event Path

目标：

- 在 `test-apps/flow-over-ipc-smoke-app` 或新增 focused validation app 中加入真实 Flow
  `TextField` path。
- 组件必须由普通 Flow Java API 创建，并注册 Java listener。
- WebView 中事件必须来自真实 `vaadin-text-field` 客户端行为，而不是原生 input baseline、
  手写 JS 直接改 Java state 或旧 shim 成功路径。
- 事件链路必须经过：
  - visible Flow client engine
  - IPC-backed XHR / message bridge
  - Swift host
  - Java sidecar
  - `UidlRequestHandler` / `ServerRpcHandler`
  - Java listener
  - UIDL patch
  - visible Flow client DOM update。

验收证据：

- Trace 包含真实 TextField component event RPC payload，能和 native input baseline 区分。
- Java listener 产生 UIDL patch，更新可见状态，例如
  `Vaadin TextField: Flow IPC typed`。
- `webview.dom` 记录 TextField rendered state、post-event state、visible owner、
  dispatch mode 和 component id。
- Inspector v1.7 gate 能区分：
  - native input baseline
  - BigDecimalField path
  - real TextField path
  - legacy `@vaadin/text-field` shim fallback。

自动化 hook 可以新增或复用明确命名的验证变量，例如：

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1
```

该 hook 只能触发验证，不能成为 runtime 正常路径。

### 4. Shim Retirement From Success Path

目标：

- 让 BigDecimalField / TextField 的成功路径不再依赖
  `META-INF/vatori/frontend-shims/vaadin-text-field.js`。
- 如果 shim 文件仍保留，必须仅作为 legacy fallback 或 regression guard。
- Manifest、trace 和 inspector 必须明确区分：
  - real TextField graph execution
  - focused partial bridge
  - legacy shim fallback
  - strict blocker。

验收证据：

- v1.7 strict gate 不接受 `META-INF/vatori/frontend-shims/vaadin-text-field.js`
  作为 TextField/npm graph 成功证据。
- `webview.dom` 能显示旧 shim 是否仍存在，以及它是否被当前成功路径使用。
- 文档说明 shim 的剩余用途和退出条件。

### 5. IPC Protocol v1.7 And Inspector Gate

目标：

- 新增或扩展 inspector 参数：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
```

- 保持以下命令继续通过：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

- v1.7 strict gate 至少要求：
  - no-localhost `transport=flow-ipc`
  - v1.6 staging and TextField graph metadata baseline
  - TextField production side-effect metadata
  - side-effect split / module identity / registry dedupe evidence
  - real TextField graph executed successfully，或 blocker moved beyond v1.6 duplicate
    custom element layer with `blockerVersion=flow-ipc-textfield-v1_7`
  - old `@vaadin/text-field` shim not counted as success
  - real Flow `TextField` event path if TextField graph executes
  - existing Button / BigDecimalField / Checkbox / Select / list / capability baseline
    still passing
  - failure classification fields for every not-executed TextField dependency。

交付：

- 新增 `docs/traces/flow-ipc-v1_7.ndjson`。
- 更新 `docs/ipc-protocol-v1.md`，必要时新增 `docs/ipc-protocol-v1_7.md`。
- `DesktopTraceInspector` 支持 `--require-flow-ipc-v1_7`。

### 6. Verification Apps

目标：

- 保留 `test-apps/flow-over-ipc-smoke-app` 作为 strict fixture source。
- 保留 `test-apps/flow-over-ipc-framework-app` 作为普通开发者形态验证。
- 允许新增 `test-apps/flow-over-ipc-textfield-app` 或等价 focused app，但只有在隔离
  TextField/npm graph 更清晰时才新增。

验证 app 必须保持：

- 普通 Flow Java API。
- Button baseline。
- BigDecimalField baseline。
- Checkbox 和 Select baseline。
- TextField path 或 TextField post-v1.6 exact blocker path。
- Repeated/list update baseline。
- 至少一个 manifest-gated native capability call。

### 7. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或 `docs/ipc-protocol-v1_7.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `docs/goal.md` 的长期未完成账本。

文档必须明确：

- v1.6 与 v1.7 的差异。
- TextField production `indexhtml` side effect 如何被拆解或去重。
- 真实 TextField graph 哪些 artifact 已执行。
- 旧 `@vaadin/text-field` shim 是否仍存在；若存在，它为什么只是 fallback。
- 如果仍有 blocker，它已经从 v1.6 duplicate custom element layer 进入哪个新层。

## 验收命令

最小验收集合：

```bash
git diff --check
bash bin/flow-client-resource-smoke
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
```

真实窗口 smoke 验证：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

framework app 回归：

```bash
VAADIN_DESKTOP_MAVEN_OFFLINE=1 VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

## 明确非目标

- 不要求完整 generated/lazy `Flow.loadOnDemand` execution，除非它是 TextField graph
  必需路径。
- 不要求完整 dev-mode/Vite runtime。
- 不要求 broader production bundle parity。
- 不要求 Grid/VirtualList、ComboBox、DatePicker、Dialog、Upload 等 broader component
  parity。
- 不要求 Windows/Linux host。
- 不要求生产打包、签名、公证或自动更新。

这些仍保留在 `docs/goal.md` 的长期未完成账本中。
