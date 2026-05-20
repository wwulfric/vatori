# Goal 1.3: Framework-Grade Flow IPC Runtime Generalization

`docs/goal.md` 可视为 **goal-1_0**，`docs/goal-1_1.md` 可视为
**goal-1_1**，`docs/goal-1_2.md` 可视为 **goal-1_2**。截至 v1.2，仓库已经证明
no-localhost Flow IPC runtime 可以运行真实 Vaadin Flow app，并且至少一个真实 Vaadin
Web Component 的 frontend resource、client event、Java listener、UIDL patch 和可见
Flow client DOM update 能脱离 localhost HTTP 闭环。

本阶段目标不是继续堆叠 smoke-only 诊断字段，也不是只增加另一个组件样例；而是把当前
受控 smoke 验证推进为更通用的框架运行时，使普通 Vaadin Flow desktop app 可以依赖框架层的
WebView shell、frontend asset resolver、Flow client bridge 和 native host 生命周期，而不是依赖测试应用
DOM id、测试专用 hook 或诊断 shim。

## 继承边界

### 已完成的 v1.2 能力

以下能力在 v1.3 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app` 启动 no-localhost Flow IPC runtime。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell。
- Java sidecar 与 host/WebView 通过 `VFIPC1` stdin/stdout envelope 通信。
- v1/v1.1/v1.2 trace inspector gates 继续可运行。
- Bootstrap、event、patch、push、resync、command、capabilityResult、resourceRequest/resourceResponse、
  error 和 shutdown 已有 trace coverage。
- Visible Flow client engine 负责消费 bootstrap、event patch、ordered push 和 recovery resync。
- 第一个真实 Vaadin Web Component resource 和 event path 已通过 v1.2 smoke 验证。
- 默认 macOS 窗口不置顶，Flow IPC shell 具备框架层 viewport 归一化，首屏不应空白或滚到底部才显示。

### 本阶段关键约束

- v1.3 验收路径仍然不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 不能通过恢复 Servlet/HTTP/WebSocket compatibility transport 满足验收。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC 或 host-specific JavaScript。
- 框架 runtime 的正常渲染、事件派发、push/resync delivery 和布局修复不能依赖 smoke app 的固定 DOM id、
  固定 Flow node id 或固定组件 id。
- Smoke app id 可以继续用于诊断、自动化和 inspector 断言，但不能作为框架运行时的准入条件。
- 如果仍存在 diagnostic shim，必须在 docs 和 trace 中明确标记，不能描述成 production frontend parity。

## 实现范围

### 1. 去测试应用耦合

目标：

- WebView shell promotion 使用通用 Flow client / viewport readiness，而不是 `ipc-button`、`ipc-status`、
  `ipc-list`、`ipc-event-matrix` 等 smoke app id。
- Push/resync delivery 只依赖 Flow client engine readiness、session/ui/client metadata 和可见 viewport，
  不依赖 smoke DOM 绑定。
- IPC-backed XHR bridge 不把缺失 RPC node 默认映射到 `ipc-button` 或固定 node id。
- 保留 smoke-specific diagnostics，但命名和文档要清楚表明它们是验证层，而不是 runtime dependency。

验收证据：

- Swift shell 中可见 renderer promotion 与 push/resync delivery 的 gate 不再读取 smoke app DOM id。
- Trace/DOM diagnostics 暴露 generic viewport readiness，例如 `flowClientEngineViewportReady` 和
  `flowClientEnginePromotionGate`。
- v1/v1.1/v1.2 gates 继续通过，证明旧 smoke 验证没有回退。

### 2. Frontend Asset Resolver v1.3

目标：

- 从 v1.2 的单个 Web Component resource 执行推进到可复用 resolver。
- 明确 classpath Flow client resource、npm/frontend module、theme/static resource、production bundle/dev-mode
  resource 的解析策略。
- 收敛 diagnostic shim；仍需保留的 shim 必须有 `blockedReason` / `failureClassification`。
- 资源 request/response trace 必须能区分 manifest graph、lazy lookup、component frontend、theme/static 和
  production/dev-mode gap。

### 3. 通用 Flow Client Bridge

目标：

- Event、patch、push、resync 逻辑按 Flow sessionId、uiId、security key、syncId、clientId 和 Flow RPC payload 工作。
- 不靠 smoke app 的固定 DOM 结构推断 runtime 状态。
- 对 resource miss、client binding failure、event dispatch failure 和 patch delivery failure 给出稳定错误分类。

### 4. WebView Shell / Native Host 框架化

目标：

- First paint、viewport、scroll、window level、sidecar cleanup 作为 host 框架默认行为。
- 增加 ready / first meaningful paint / visible renderer promotion trace，避免白屏类问题回归。
- 默认窗口不置顶；未来如需置顶，作为显式 app/window 配置，而不是默认行为。

### 5. 普通开发者形态验证 app

目标：

- 保留 `test-apps/flow-over-ipc-smoke-app` 作为严格回归 smoke。
- 新增或扩展一个更接近普通 Vaadin app 的验证工程，要求只写普通 Flow Java API。
- 至少覆盖 Button、两个真实 Vaadin Web Component、一个 repeated/list 更新路径和一个 native capability 调用。

## 验收标准

1. v1 fixture gate 继续通过。
2. v1.1 fixture gate 继续通过。
3. v1.2 fixture gate 继续通过。
4. 新增 v1.3 strict gate，例如：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
```

5. 真实 macOS `dev-ipc` 窗口运行通过，无 localhost HTTP。
6. 框架 runtime 渲染、visible renderer promotion、push/resync delivery 和布局修复不依赖测试应用 id。
7. 至少两个真实 Vaadin Web Component 通过 generic resource resolver 和 Flow client bridge 闭环。
8. Trace 能证明 frontend resource resolution、event RPC、Java listener、UIDL patch 和 visible DOM update。
9. Docs 明确哪些是框架能力，哪些只是 smoke/diagnostic。
10. 退出后没有 Java sidecar 或 native host 残留。
11. `git diff --check`、Swift compile、Maven compile 通过。
12. 按适合粒度提交 commit。

## 非目标

- 不要求一次完成 production bundle/dev-mode replacement。
- 不要求完整 Vaadin component parity。
- 不要求完整 Grid/VirtualList。
- 不要求完整 theme/resource graph parity。
- 不要求生产级签名、公证、安装器、自动更新。
- 不要求 Windows/Linux host。
- 不删除 compatibility mode。

## 停止条件

当前仓库形成一个可运行、可评审的 **Flow IPC v1.3 framework-grade runtime slice**：

- 普通 Vaadin Flow app 可以依赖框架层 WebView shell 和 Flow client bridge 进入可见 renderer。
- Runtime 正常路径不再被 smoke app DOM id、固定 node id 或测试 hook 卡住。
- 至少两个真实 Vaadin Web Component 的资源解析、事件 RPC、Java listener、UIDL patch 和可见 DOM update
  都通过 Flow IPC runtime 完成。
- v1/v1.1/v1.2/v1.3 trace gates 和真实窗口 host log 都有 strict inspector 证据。
- 文档清楚说明已完成的框架能力、仍保留的 diagnostic shim、blocked/fallback 资源和下一阶段拆解。

## 关键约束

本 goal 的关键验收约束是：

**把 v1.2 的单点 Web Component 成功推进为框架级通用运行时能力；测试应用只能验证框架，不允许成为框架运行时的隐式依赖。**
