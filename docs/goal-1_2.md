# Goal 1.2: First Real Vaadin Web Component Over Flow IPC

`docs/goal.md` 可视为 **goal-1_0**，`docs/goal-1_1.md` 可视为
**goal-1_1**。截至 v1.1，仓库已经具备可运行、可评审的 no-localhost
`dev-ipc` runtime，并完成了 capability gateway、文件传输、基础非 Button 事件、
push ordering / recovery resync，以及 Flow client resource classification /
lazy lookup diagnostics。

本阶段目标不是继续扩展诊断字段，也不是回到 localhost compatibility mode；而是把
当前受控 smoke runtime 推进到一个关键产品化边界：

**至少一个真实 Vaadin Web Component 组件及其前端资源，在 no-localhost
Flow IPC runtime 中可加载、可执行、可交互，并通过真实 Flow UIDL RPC 路径触发
Java listener 与可见 Flow client DOM 更新。**

优先组件建议为 `com.vaadin.flow.component.textfield.TextField`。如果 TextField
暴露出不可控的 frontend/dev-mode 依赖，可选择同等级别的真实 Vaadin Web Component
组件作为替代，但不能用 `com.vaadin.flow.component.html.Input` 作为本阶段验收主体。

## 继承边界

### 已完成的 v1.1 能力

以下能力在 v1.2 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app`
  启动 no-localhost Flow IPC runtime。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell。
- Java sidecar 与 host/WebView 通过 `VFIPC1` stdin/stdout envelope 通信。
- Bootstrap、event、patch、push、resync、command、capabilityResult、error、shutdown
  已有 v1/v1.1 trace coverage。
- Bootstrap、ordered push 和 recovery resync 使用真实 Vaadin UIDL 输出。
- Button click、native `Input` text change、native checkbox change 已通过
  visible Flow client engine -> IPC-backed XHR -> `UidlRequestHandler` ->
  visible Flow client patch 路径验证。
- `FlowBootstrap.js` 和 augmented `FlowClient.js` 通过 IPC 加载、导入、初始化。
- 其余 advertised Flow client resources 已能 request、verify、classify，并有一个
  `Flow.js` lazy lookup 诊断。
- `DesktopTraceInspector --require-flow-ipc-v1` 与 `--require-flow-ipc-v1_1`
  都必须继续通过。

### 本阶段关键约束

- v1.2 验收路径仍然不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 不能通过恢复 Servlet/HTTP/WebSocket compatibility transport 满足验收。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC。
- 不直接修改本地 upstream Vaadin 源码；如需 fork/spike，先放在本仓库并明确边界。
- 现有 `html.Input` / checkbox smoke 可作为回归基线，但不能替代真实 Vaadin
  Web Component 验收。
- 如果某个资源或组件失败，trace 和文档必须给出明确失败分类、缺失资源、下一步拆解，
  不能把 fallback 描述成成功。

## 实现范围

### 1. Frontend Asset Resolver v1.2

v1.1 已经能验证和分类 Flow client classpath resources，但还没有执行真实 Vaadin
component frontend module。v1.2 要建立第一个可评审的前端资源解析边界。

目标：

- 为 WebView/host 建立真实 Vaadin component frontend module 的 no-localhost 解析路径。
- 优先评估并选择一种最小可行实现：
  - `WKURLSchemeHandler` / custom scheme，例如 `flow-ipc-resource://...`
  - IPC-backed module/resource request
  - app bundle 或 `.build` staging 后由 WebView 本地加载
  - production bundle 中提取出的最小 component asset graph
- 至少解析并执行一个真实 Vaadin Web Component 所需的 frontend resource，例如
  TextField 相关 module。
- 对每个资源记录：
  - requested
  - resolved
  - verified
  - imported/executed
  - blocked 或 failed reason
- 明确区分 classpath Flow client resource、npm/frontend generated resource、
  theme/static resource 和 production bundle/dev-mode resource。

验收证据：

- trace 中能看到真实 Vaadin component frontend resource lookup，不只是
  `FlowBootstrap.js` / `FlowClient.js` / `Flow.js`。
- `webview.dom` 或 inspector 输出能看到至少一个 component frontend resource
  `executed=true`。
- 对 unresolved transitive imports、theme resources 或 dev-mode resources 有显式分类。

### 2. Real Vaadin Web Component Event Path

v1.2 的核心闭环必须来自真实 Vaadin Web Component，而不是原生 HTML input fallback。

目标：

- 扩展或新增 smoke view，包含至少一个真实 Vaadin Web Component，优先：
  - `TextField`
  - 或 `Checkbox`
  - 或 `Select`
- 组件必须由普通 Flow Java API 创建，并注册 Java listener。
- WebView 中可见 Flow client engine 必须渲染该组件，事件必须从该组件的客户端行为触发。
- 事件必须经过：
  - visible Flow client engine
  - IPC-backed XHR / bridge
  - Swift host
  - Java sidecar
  - `UidlRequestHandler` / `ServerRpcHandler`
  - Java listener
  - UIDL patch
  - visible Flow client DOM update
- 不能用手写 JS 直接伪造 Java-side state change 作为成功路径。

验收证据：

- trace 中包含该真实 Vaadin Web Component 的 event RPC payload。
- Java listener 产生的 UIDL patch 包含可验证状态，例如
  `Vaadin TextField: Flow IPC typed`。
- `webview.dom` 记录组件 rendered state、post-event state 和 visible owner。
- inspector v1.2 gate 能严格区分这个路径和 v1.1 的 `html.Input` baseline。

### 3. Component Resource Failure Classification

如果真实 Vaadin Web Component 的完整资源图不能一次性跑通，本阶段仍必须产出有用的
工程证据，而不是停在“资源加载失败”。

要求：

- 记录失败发生在哪一层：
  - module specifier resolution
  - classpath lookup
  - npm/frontend artifact missing
  - dynamic import failure
  - browser execution error
  - Flow client binding failure
  - event dispatch failure
- trace 中给出 `blockedReason` 或 `failureReason`。
- docs 中明确下一步是 production bundle、dev-mode replacement、还是 component-specific
  adapter。
- 只有真实组件成功渲染并触发 listener，才算本阶段核心验收通过；失败分类用于辅助拆解，
  不能单独满足完成条件。

### 4. IPC Protocol v1.2 And Inspector Gate

在 v1/v1.1 基础上增加 v1.2 资源与组件证据，不破坏旧 fixture。

目标：

- 新增或扩展 inspector 参数：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
```

- 保持以下命令继续通过：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
```

- v1.2 strict gate 至少要求：
  - no-localhost `transport=flow-ipc`
  - v1.1 baseline evidence
  - real Vaadin Web Component resource lookup
  - component frontend resource executed evidence
  - real Vaadin Web Component event RPC evidence
  - Java listener UIDL patch evidence
  - visible Flow client DOM post-event evidence
  - explicit failure/block classification fields present

交付：

- 新增 `docs/traces/flow-ipc-v1_2.ndjson`。
- 更新 `docs/ipc-protocol-v1.md`，或在必要时新增 `docs/ipc-protocol-v1_2.md`。
- `DesktopTraceInspector` 支持 `--require-flow-ipc-v1_2`。

### 5. Smoke App And Verification Hooks

允许两种路线：

- 扩展 `test-apps/flow-over-ipc-smoke-app`。
- 或新增 `test-apps/flow-over-ipc-v1_2-smoke-app`，如果隔离 v1.2 资源依赖更清晰。

测试工程必须保持普通开发者项目形态，并包含：

- `@Route("")`
- v1.1 baseline controls
- 至少一个真实 Vaadin Web Component
- 对应 Java listener 和 visible state update
- 自动化 verification hook，能在真实 macOS 窗口中重复触发该组件事件
- resource diagnostics UI/trace evidence

真实窗口验证命令应保持或扩展：

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 \
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

可新增 v1.2 专用 hook，例如：

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1
```

### 6. Documentation

更新或新增：

- `README.md`
- `AGENTS.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或 `docs/ipc-protocol-v1_2.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- smoke app README

文档必须明确：

- v1.1 与 v1.2 的差异。
- 真实 Vaadin Web Component 成功路径覆盖到哪里。
- 哪些 resource 仍是 verified-only / intentionally-not-executed / blocked / failed。
- production bundle 与 dev mode 尚未完成的边界。
- 为什么该路径仍然不是 localhost WebView wrapper。
- 下一阶段如何继续推进 Grid/VirtualList、theme、production bundle、dev-mode replacement、
  durable push/reconnect 和 packaging。

## 验收标准

1. v1 fixture gate 继续通过。
2. v1.1 fixture gate 继续通过。
3. 新增 v1.2 fixture gate，并且 `--require-flow-ipc-v1_2` 通过。
4. 真实 macOS window 运行 no-localhost `dev-ipc` smoke，并且 host log 通过 v1.2 strict gate。
5. trace 中能看到至少一个真实 Vaadin Web Component frontend resource 被 no-localhost
   resource resolver 请求、解析、验证并执行。
6. trace 中能看到该真实 Vaadin Web Component 产生 Flow-shaped event RPC。
7. Java listener 通过真实 Flow server RPC 路径触发，并返回 UIDL patch。
8. visible Flow client engine 消费 patch，`webview.dom` 能看到 post-event state。
9. v1.1 的 Button、native input/checkbox、push ordering/resync、file dialog、file transfer、
   permission deny-path 证据不回退。
10. `git diff --check`、Swift compile、Maven compile 通过。
11. 真实窗口运行结束后没有残留 Java sidecar/native host 进程。
12. 按适合粒度提交 commit。

## 非目标

- 不要求完整 Vaadin component parity。
- 不要求完整 Grid/VirtualList。
- 不要求完整 production bundle/dev-mode replacement。
- 不要求完整 theme/resource graph parity。
- 不要求完整 Flow Push/WebSocket parity 或 durable reconnect。
- 不扩展 upload/download streaming/progress/cancel。
- 不要求 Windows/Linux host。
- 不删除 compatibility mode。

## 停止条件

当前仓库形成一个可运行、可评审的 **Flow IPC v1.2 real Vaadin Web Component slice**：

- 工程师可以用明确命令启动真实 macOS native window。
- 至少一个真实 Vaadin Web Component 的 frontend resource 和 event path 脱离 localhost HTTP。
- Java listener、UIDL patch、visible Flow client DOM update 都通过 Flow IPC runtime 完成。
- v1/v1.1/v1.2 trace fixtures 和 real-window host log 都有 strict inspector gate。
- 文档清楚说明已完成能力、blocked/fallback 资源、仍未完成能力和下一阶段拆解。

## 关键约束

本 goal 的关键验收约束是：

**v1.2 必须证明真实 Vaadin Web Component 的前端资源执行与事件闭环可以进入
no-localhost Flow IPC runtime；不能再用原生 HTML input fallback 或 localhost
compatibility transport 作为核心验收路径。**
