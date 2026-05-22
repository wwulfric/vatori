# Goal 1.6: TextField / npm Graph Parity

`docs/goal.md` 是长期愿景和完成度账本；`docs/goal-1_0.md` 到
`docs/goal-1_5.md` 是已完成阶段快照。v1.5 已经把 Flow IPC frontend artifact
pipeline 推进到 no-localhost staged artifact graph：真实 artifact body 可以被 stage
为 Blob module URL，`resourceStage` trace 能记录 staged、executed、failed 和 strict
blocker，BigDecimalField 与 focused Checkbox/Select entries 已通过 staged path 执行。

本阶段目标聚焦长期账本里最直接阻塞完整 frontend parity 的缺口：

**替换或严格拆解 `@vaadin/text-field` / npm frontend graph 的剩余 shim，使
TextField 相关真实 Vaadin artifact graph 不再只停留在 v1.5 的 blocker 证据上。**

v1.6 不是继续扩大 smoke-only 断言，也不是添加新的 WebView inline shim。完成后，
`dev-ipc` 应能证明 TextField/npm graph 通过 no-localhost staged graph 被真实执行，
或被压缩为比 v1.5 更小、更精确、可复现且可继续拆解的 staged module blocker。

## 继承边界

### 已完成的 v1.5 能力

以下能力在 v1.6 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app`
  继续启动 no-localhost Flow IPC smoke runtime。
- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app`
  继续启动普通开发者项目形态的 framework validation app。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell，不通过 localhost
  HTTP URL 加载核心 UI path。
- Visible Flow client engine 通过 generic viewport promotion 负责可见 renderer。
- v1/v1.1/v1.2/v1.3/v1.4/v1.5 fixture gates 和
  `--require-flow-ipc-v1_3-framework` host-log gate 继续可运行。
- Flow client classpath、component frontend、npm/frontend、theme/static、production bundle、
  dev-mode bundle 和 generated/lazy artifact families 都能通过 manifest 表达和验证。
- BigDecimalField、Checkbox、Select、Button、list/repeated update 和 native capability
  baseline 继续可验证。
- v1.5 staging resolver 能记录 `resourceStage`，并在 `webview.dom` 中区分
  staged、executed、failed 和 strict blocker resource。
- TextField production class entry 已能通过 staging 尝试，当前 blocker 至少记录到：
  `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js`、相关 transitive dependencies
  和浏览器错误。
- 旧 `@vaadin/text-field` shim 只允许作为回归保护，不能作为 v1.6 成功证据。

### 本阶段关键约束

- v1.6 验收路径不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 不能恢复 Servlet/HTTP/WebSocket compatibility transport 来满足验收。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC 或 host-specific JS。
- 不允许把 TextField/npm graph 支持实现成新的泛化不明 inline shim。
- 不能把 v1.5 已有的 `"Cannot define multiple custom elements with the same tag name"`
  blocker 原样搬运为 v1.6 完成证据；v1.6 必须执行真实 TextField graph，或显著缩小并
  精确化该 blocker。
- 如需保留任何 shim，必须明确它只用于旧路径回归保护，并且 inspector 不把它计入成功。

## 实现范围

### 1. TextField / npm Artifact Graph Inventory

目标：

- 建立 TextField graph 的可审计清单，从 Flow Java component 到 frontend module、
  npm module specifier、production/dev bundle chunk、focused extracted entry 和 staged URL。
- 基于本地 Vaadin 源码和仓库内 dev-bundle artifacts 梳理至少以下依赖层：
  - `@vaadin/text-field/src/vaadin-text-field.js`
  - TextField production class entry
  - `indexhtml` production chunk 中的 TextField class
  - `@vaadin/component-base`
  - `@vaadin/field-base` / input-container / field-highlighter 等直接相关依赖
  - `lit` / CSS helpers
  - relative imports 和 dynamic imports
- 对每个 TextField 相关 artifact 记录：
  - source path
  - module specifier
  - artifact family
  - sha256 / bytes
  - dependencyOf / requestedBy
  - resolver
  - execution policy
  - current status
  - blockedReason / failureClassification。

验收证据：

- `FlowClientResourceSmokeMain` 或等价 smoke 能检查 TextField/npm graph metadata。
- Trace 中的 `resourceRequest` / `resourceResponse` 能说明 TextField graph 的 artifact
  family、specifier、resolver 和 dependency chain。
- 文档中能区分真实 TextField artifact、focused partial bridge、旧 shim 和 blocker。

### 2. Staged TextField Module Graph Resolver

目标：

- 在 v1.5 staging resolver 基础上，让 TextField/npm graph 使用同一套可复用 staged
  module graph 路径，而不是特殊分支或 inline shim。
- 优先推进以下方向之一：
  - custom scheme / `WKURLSchemeHandler`，使 relative imports 可以稳定解析。
  - staged file URL 或 app-bundle resource URL 加 import map / specifier rewrite。
  - Blob module graph rewrite，但必须解决 relative import、custom element duplicate
    和 cleanup 生命周期。
- 处理或记录 custom element registry 冲突：
  - 哪个 tag 已被定义。
  - 首次定义来自哪个 artifact。
  - 第二次定义来自哪个 artifact。
  - 是否由 BigDecimalField、TextField direct replacement、generated imports 或 production
    chunk bootstrap order 引发。
- 不能通过“空实现 TextField class”或“WebView 专用伪组件”绕过真实 Vaadin artifact。

验收证据：

- Trace 中出现 TextField graph 的 staged module URL、cache hit/miss、cleanup policy、
  execution attempt 和 outcome。
- 若成功执行，`webview.dom` 记录 TextField staged graph executed。
- 若失败，`resourceStage` 记录具体 staged module、transitive dependency、browser error、
  failure classification、custom element registry state 和 nextResolutionStep。

### 3. Real TextField Component Event Path

目标：

- 在 smoke app 或新增 focused validation app 中加入真实 Flow `TextField` path。
- 组件必须由普通 Flow Java API 创建，并注册 Java listener。
- WebView 中事件必须来自真实 TextField 客户端行为，而不是手写 JS 直接修改 Java state。
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

- Trace 包含真实 TextField component event RPC payload。
- Java listener 产生 UIDL patch，更新可见状态，例如 `Vaadin TextField: Flow IPC typed`。
- `webview.dom` 记录 TextField rendered state、post-event state、visible owner 和
  dispatch mode。
- Inspector v1.6 gate 能区分真实 TextField path、BigDecimalField path 和旧 native input
  baseline。

### 4. Exact Blocker Reduction Path

如果真实 TextField graph 仍无法执行，本阶段仍可完成，但必须产出比 v1.5 更强的工程证据。
不能只复述旧 blocker。

要求：

- 建立一个最小 staged graph reproduction，明确：
  - entry artifact
  - transitive artifact
  - import specifier rewrite 或 URL resolution 策略
  - execution order
  - custom element registry state
  - browser exception
  - Vaadin/WebKit/opaque-origin 相关性判断
  - 下一步拆解路径。
- blocker 必须收敛到具体层：
  - module specifier resolution
  - relative import URL resolution
  - duplicate custom element definition
  - production chunk side effect
  - generated imports bootstrap order
  - Flow client API / bootstrap config 缺口
  - WebKit opaque origin / CSP / module loader 限制。
- v1.6 strict gate 必须能判断这是新 blocker 证据，而不是 v1.5 blocker 原样重复。

验收证据：

- `resourceStage` 或等价 Host Protocol 事件包含 `blockerVersion=flow-ipc-textfield-v1_6`
  或等价可识别字段。
- `webview.dom` 或 inspector summary 能显示 TextField blocker 已被 narrowed。
- 文档给出下一阶段可直接执行的拆解任务，而不是只说 “TextField still blocked”。

### 5. IPC Protocol v1.6 And Inspector Gate

目标：

- 新增或扩展 inspector 参数：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
```

- 保持以下命令继续通过：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

- v1.6 strict gate 至少要求：
  - no-localhost `transport=flow-ipc`
  - v1.5 staging baseline
  - TextField/npm graph request/response metadata
  - TextField staged execution attempt beyond v1.5
  - TextField graph executed successfully，或 exact narrowed blocker evidence
  - old `@vaadin/text-field` shim not counted as success
  - existing Button / BigDecimalField / Checkbox / Select / list / capability baseline still passing
  - failure classification fields for every not-executed TextField dependency。

交付：

- 新增 `docs/traces/flow-ipc-v1_6.ndjson`。
- 更新 `docs/ipc-protocol-v1.md`，必要时新增 `docs/ipc-protocol-v1_6.md`。
- `DesktopTraceInspector` 支持 `--require-flow-ipc-v1_6`。

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
- TextField path 或 TextField exact blocker path。
- Repeated/list update baseline。
- 至少一个 manifest-gated native capability call。

自动化 hook 只能触发验证，不能成为 runtime 正常路径。可新增：

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1
```

或等价明确命名的 hook，用于触发真实 TextField event path。

### 7. Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或 `docs/ipc-protocol-v1_6.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- 必要时更新 `docs/goal.md` 的长期未完成账本。

文档必须明确：

- v1.5 与 v1.6 的差异。
- TextField/npm graph 哪些 artifact 已真实执行，哪些仍是 focused partial bridge。
- 旧 `@vaadin/text-field` shim 是否仍存在；若存在，它为什么只是回归保护。
- 如果仍失败，失败定位在哪一层，下一阶段如何拆。
- 为什么该路径仍然不是 localhost WebView wrapper。

## 验收标准

1. v1 fixture gate 继续通过。
2. v1.1 fixture gate 继续通过。
3. v1.2 fixture gate 继续通过。
4. v1.3 fixture gate 继续通过。
5. v1.4 fixture gate 继续通过。
6. v1.5 fixture gate 继续通过。
7. `--require-flow-ipc-v1_3-framework` host-log gate 继续通过。
8. 新增 v1.6 fixture gate，并且 `--require-flow-ipc-v1_6` 通过。
9. 真实 macOS `dev-ipc` 窗口运行通过，无 localhost HTTP。
10. TextField/npm graph 有真实 staged execution，或比 v1.5 更精确的 exact narrowed blocker。
11. 旧 `@vaadin/text-field` shim 不被计入 v1.6 成功证据。
12. 如果 TextField 事件成功，必须验证 TextField event RPC -> Java listener ->
    UIDL patch -> visible DOM update。
13. 如果 TextField 仍失败，必须有 minimal staged graph reproduction、browser error、
    custom element registry state、failure classification 和 nextResolutionStep。
14. BigDecimalField、Checkbox、Select、Button、list update 和 native capability baseline
    不回退。
15. `git diff --check`、Swift compile、Maven compile、resource smoke 通过。
16. 真实窗口运行结束后没有 Java sidecar 或 native host 残留。
17. 按适合粒度提交 Conventional Commit。

## 非目标

- 不要求一次完成完整 Vaadin component parity。
- 不要求完整 Grid/VirtualList。
- 不要求完整 production bundle 或 dev-mode/Vite execution。
- 不要求完整 generated/lazy `loadOnDemand` execution，除非它是 TextField graph 必需路径。
- 不要求 Windows/Linux host。
- 不删除 compatibility mode。
- 不要求本阶段 upstream 到 Vaadin Flow。

## 停止条件

当前仓库形成一个可运行、可评审的 **Flow IPC v1.6 TextField/npm graph parity slice**：

- TextField/npm graph 不再只有 v1.5 的泛化 shim/blocker 证据。
- `dev-ipc` 能通过 no-localhost staging resolver 执行真实 TextField graph，或给出
  exact narrowed blocker，且该 blocker 已缩小到具体 staged module / dependency /
  WebView error / registry state。
- v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6 trace gates 和真实窗口 host log 都有 strict
  inspector 证据。
- 文档清楚说明 TextField graph 当前完成度、剩余 partial bridge / diagnostic shim /
  blocked resource，以及下一阶段可直接拆解的任务。

## 关键约束

本 goal 的关键验收约束是：

**TextField/npm graph 必须从 v1.5 的 staged blocker 继续向前推进：要么执行真实
TextField artifact graph，要么把 blocker 缩成可复现、可继续拆解的更小 staged graph。
不能把旧 shim 或旧 blocker 原样包装成 v1.6 成功。**
