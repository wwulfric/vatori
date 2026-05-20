# Goal 1.4: Flow IPC Frontend Asset Pipeline

`docs/goal.md` 可视为 **goal-1_0**，`docs/goal-1_1.md` 可视为
**goal-1_1**，`docs/goal-1_2.md` 可视为 **goal-1_2**，
`docs/goal-1_3.md` 可视为 **goal-1_3**。截至 v1.3，仓库已经把
no-localhost Flow IPC runtime 从受控 smoke 验证推进到更通用的框架运行时：
普通 Vaadin Flow app 可以依赖框架层 WebView shell、generic viewport promotion、
Flow client bridge、framework-shaped validation app 和 dedicated host-log gate。

本阶段目标不是继续扩大 smoke-only 断言，也不是再手写一个组件专用 shim；而是把
v1.3 的 focused dev-bundle entry / BigDecimalField shims 推进为更系统的
**Flow IPC frontend asset pipeline**，使 `dev-ipc` 可以按可审计的 Vaadin frontend
artifact graph 加载 Flow client、组件 frontend module、theme/static resource、
production bundle 和 dev-mode bundle 资源。

## 继承边界

### 已完成的 v1.3 能力

以下能力在 v1.4 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app` 继续启动 no-localhost
  Flow IPC smoke runtime。
- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app` 继续启动更接近普通
  开发者项目的 framework validation app。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell，不通过 localhost
  HTTP URL 加载核心 UI path。
- Visible Flow client engine 通过 `generic-flow-viewport` 负责可见 renderer promotion。
- `webview.firstMeaningfulPaint`、viewport normalization、默认 `.normal` window level 和
  sidecar cleanup 已纳入 host 框架默认行为。
- v1/v1.1/v1.2/v1.3 fixture gates 和 `--require-flow-ipc-v1_3-framework` host-log gate
  已可运行。
- `BigDecimalField` classpath frontend module 和 `Checkbox` focused dev-bundle entry 已能通过
  IPC resource resolver 执行，并完成 event RPC -> Java listener -> UIDL patch ->
  visible DOM update。
- 当前 BigDecimalField 的 `lit`、`@vaadin/text-field`、`@vaadin/component-base` 仍是明确标记的
  v1.2 diagnostic shims；Checkbox 走 focused Vaadin dev-bundle entry，但还不是完整
  production bundle/dev-mode parity。

### 本阶段关键约束

- v1.4 验收路径仍然不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 不能通过恢复 Servlet/HTTP/WebSocket compatibility transport 满足验收。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC 或 host-specific JavaScript。
- 新的 frontend resolver 不能把组件支持写死为 `vaadin-big-decimal-field.js` 或
  `META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js` 的专用逻辑；这些路径可以作为
  回归样例，但不能继续作为 resolver 架构。
- 如果无法在本阶段完全消除某类 shim，必须把 blocker 定位到具体 artifact 类型：
  Maven classpath 缺失、npm frontend artifact 缺失、generated bundle 缺失、theme/static
  lookup 缺失、opaque-origin/WebView 限制或 Flow client dynamic import 限制。

## 实现范围

### 1. Frontend Resource Manifest v1.4

目标：

- 在 `vaadin-desktop-flow` 中定义更清晰的 frontend resource manifest。
- Manifest 至少区分：
  - Flow client classpath resource
  - Vaadin component classpath frontend module
  - npm/frontend module
  - theme/static resource
  - production bundle resource
  - dev-mode bundle resource
  - generated/lazy resource
- 每个 resource entry 至少包含：
  - `path`
  - `kind`
  - `origin`
  - `moduleSpecifier`
  - `sha256`
  - `bytes`
  - `dependencyOf`
  - `executionPolicy`
  - `resolver`
  - `blockedReason` / `failureClassification`，如果未完整支持。

验收证据：

- `resourceRequest` / `resourceResponse` trace 可以说明资源属于哪个 artifact family。
- `webview.dom` 或 inspector summary 能区分 resolved、verified、executed、
  intentionally-not-executed、blocked 和 failed。
- 旧 v1.3 manifest 兼容策略明确，不破坏历史 fixture。

### 2. Generic Module Loading In WKWebView

目标：

- Swift WebView shell 建立通用 module specifier resolution 策略，而不是只判断少数固定路径。
- 支持组件 frontend dependency graph 的递归请求和本地 module URL 重写。
- 评估并至少推进一种稳定加载路径：
  - Blob URL graph rewrite
  - custom scheme / `WKURLSchemeHandler`
  - app bundle 或 `.build` staging resource
  - IPC-backed dynamic resource request fallback
- 明确 Blob URL 生命周期、resource cache、重复请求和失败清理策略。

验收证据：

- 至少一个组件的依赖资源不再通过手写 inline shim 满足，或 trace 中给出严格 blocker。
- Trace 记录每个 dependency 的 `requestedBy` / `dependencyOf` / `moduleSpecifier`。
- Browser execution error、missing module specifier、resource miss 和 unsupported dynamic import
  有稳定分类。

### 3. Reduce Diagnostic Shims

目标：

- 优先消除 BigDecimalField 依赖的 `lit`、`@vaadin/text-field`、
  `@vaadin/component-base` diagnostic shim。
- 如果无法完全消除，必须将失败收敛为具体 artifact pipeline 缺口，而不是继续保留泛泛的
  `npm-transitive-imports-shimmed`。
- Checkbox focused dev-bundle entry 可以保留为过渡路径，但需要纳入 manifest graph，而不是作为
  WebView shell 的特殊分支。

验收证据：

- `FlowClientResourceSmokeMain` 或等价 smoke 能验证 v1.4 manifest metadata。
- Inspector v1.4 gate 能区分：
  - real artifact graph success
  - focused dev-bundle partial success
  - explicit shim fallback
  - hard blocker。
- 文档列出仍保留的 shim、原因和下一步替代路径。

### 4. Theme / Static / Lazy Resource Path

目标：

- v1.3 的 `Flow.loadOnDemand` 和 theme/static 仍偏诊断；v1.4 要至少推进一个真实解析路径。
- 优先验证：
  - 一个 theme/static resource lookup
  - 一个 generated/lazy resource lookup
  - 或一个 production/dev bundle chunk lookup
- 如果真实资源暂不可用，必须通过 fixture 和 docs 给出可复现 blocker。

验收证据：

- Trace 中出现非 probe-only 的 lazy/static/theme resource request/response，或严格 blocker。
- `webview.dom` 记录 lookup result，不只显示 `no-theme-resource-declared-by-smoke-app`。
- Inspector v1.4 gate 覆盖该证据。

### 5. Frontend-Heavy Verification App

目标：

- 保留 `test-apps/flow-over-ipc-smoke-app` 和 `test-apps/flow-over-ipc-framework-app`。
- 新增或扩展一个 frontend-heavy 验证路径，仍保持普通开发者项目形态。
- 至少覆盖：
  - Button
  - `BigDecimalField` 或真实 `TextField`
  - `Checkbox`
  - `Select` 或另一个真实 Vaadin Web Component
  - 一个 repeated/list update
  - 一个 native capability call

要求：

- App 只写普通 Flow Java API，不写 IPC 或 host-specific JavaScript。
- 自动化 hook 只能作为验证触发器，不能成为 runtime 正常路径。

### 6. IPC Protocol v1.4 And Inspector Gate

目标：

- 新增或扩展 inspector 参数：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
```

- 保持以下命令继续通过：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

- v1.4 strict gate 至少要求：
  - no-localhost `transport=flow-ipc`
  - generic viewport / first meaningful paint evidence
  - v1.3 baseline evidence
  - frontend resource manifest v1.4 evidence
  - dependency graph request/response evidence
  - at least one theme/static/lazy/generated lookup success or strict blocker
  - at least two real Vaadin Web Component event RPC -> Java listener -> UIDL patch -> visible DOM paths
  - explicit resource failure classification fields。

## Documentation

更新或新增：

- `README.md`
- `README.zh-CN.md`
- `AGENTS.md`
- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或 `docs/ipc-protocol-v1_4.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`

文档必须明确：

- v1.3 与 v1.4 的差异。
- 当前 frontend asset pipeline 覆盖哪些 artifact family。
- 哪些 resource 已经 resolved / verified / executed。
- 哪些 resource 仍是 focused dev-bundle partial bridge、diagnostic shim 或 blocker。
- production bundle 与 dev mode 尚未完成的边界。
- 下一阶段如何继续推进 full component parity、theme parity、packaging-time bundle staging 和
  upstream Vaadin SPI。

## 验收标准

1. v1 fixture gate 继续通过。
2. v1.1 fixture gate 继续通过。
3. v1.2 fixture gate 继续通过。
4. v1.3 fixture gate 继续通过。
5. `--require-flow-ipc-v1_3-framework` host-log gate 继续通过。
6. 新增 v1.4 fixture gate，并且 `--require-flow-ipc-v1_4` 通过。
7. 真实 macOS `dev-ipc` 窗口运行通过，无 localhost HTTP。
8. Frontend manifest 能表达 classpath、npm/frontend、theme/static、production/dev bundle、
   generated/lazy resource 的解析状态。
9. 至少一个之前依赖 diagnostic shim 的路径被真实 artifact graph 替代，或有 strict blocker
   和下一步拆解。
10. Trace 能证明 dependency graph request/response、module execution、lazy/static/theme lookup、
    event RPC、Java listener、UIDL patch 和 visible DOM update。
11. Docs 明确哪些是框架能力，哪些只是 focused dev-bundle partial bridge、smoke/diagnostic 或 blocker。
12. `git diff --check`、Swift compile、Maven compile、resource smoke 通过。
13. 真实窗口运行结束后没有 Java sidecar 或 native host 残留。
14. 按适合粒度提交 commit。

## 非目标

- 不要求一次完成完整 Vaadin component parity。
- 不要求完整 Grid/VirtualList。
- 不要求完整 Flow Push/WebSocket parity 或 durable reconnect。
- 不要求生产级签名、公证、安装器、自动更新。
- 不要求 Windows/Linux host。
- 不删除 compatibility mode。
- 不要求本阶段 upstream 到 Vaadin Flow。

## 停止条件

当前仓库形成一个可运行、可评审的 **Flow IPC v1.4 frontend asset pipeline slice**：

- `dev-ipc` 不再只靠少量组件专用路径和手写 shim 证明 frontend resource execution。
- Frontend resource manifest 能清楚表达 Flow client、component frontend、npm/frontend、
  theme/static、production/dev bundle 和 generated/lazy resource 的解析状态。
- 至少两个真实 Vaadin Web Component 继续通过 Flow IPC 完成 resource execution、event RPC、
  Java listener、UIDL patch 和 visible DOM update。
- v1/v1.1/v1.2/v1.3/v1.4 trace gates 和真实窗口 host log 都有 strict inspector 证据。
- 文档清楚说明已完成的 asset pipeline 能力、仍保留的 partial bridge / diagnostic shim、
  blocked/fallback resource 和下一阶段拆解。

## 关键约束

本 goal 的关键验收约束是：

**把 v1.3 的 focused component resource bridge 推进为可审计、可扩展的 frontend asset
pipeline；如果无法消除某个 shim，必须把它转化为明确、可复现、可继续拆解的 artifact
pipeline blocker。**
