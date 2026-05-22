# Goal: Vaadin Flow over IPC Runtime v1

基于当前 Vaadin Desktop Framework v0、Swift/AppKit native host、Host Protocol v0 和
`flow-transport-adapter-spike`，推进并实现最终的 **Flow over IPC** 架构，使 Vaadin Flow
桌面应用的核心 UI transport 从 Servlet/HTTP/WebSocket compatibility mode 迁移到本地 IPC。

目标不是继续增强 localhost WebView compatibility mode，也不是简单优化 Swift host，而是完成一个可运行、可评审、可继续产品化的 Vaadin Flow over IPC runtime。

开发者仍然编写普通 Vaadin Flow Java View，例如 `@Route`、`Button`、`Grid`、`Layout` 和事件 listener；框架负责通过本地 IPC 完成 bootstrap、client event、server-side UIDL/patch、resync/push 的传输，并由 Swift/AppKit host 管理系统 WebView、Java sidecar、窗口生命周期和 native capability gateway。

请持续推进，直到仓库中至少有一个真实 Vaadin Flow desktop app 在核心 UI 交互路径上不依赖 localhost HTTP server，并能通过真实 macOS 桌面窗口验证可用。

## 实现范围

### 1. 最终架构边界

明确并实现三方边界：

- Java Flow runtime / sidecar
- Swift/AppKit native host
- WKWebView 中的 Flow client engine / bridge

核心 UI transport 必须走 IPC：

- bootstrap
- client event dispatch
- UIDL/patch response
- resync/reload
- 最小 push 或 server-initiated update

localhost HTTP/Servlet/WebSocket 只能保留为 compatibility fallback，不能参与最终验证 app 的核心 UI 闭环。

继续保留 Vaadin Flow Java 编程模型，不要求应用开发者手写 IPC。

### 2. Flow Transport Adapter

在 `vaadin-desktop-flow` 中实现真实 `FlowTransportAdapter`，替代当前 spike。

基于本地 Vaadin 源码 `/Users/bytedance/IdeaProjects/study/vaadin` 继续定位并封装：

- bootstrap/UI 初始化路径
- `UidlRequestHandler`
- `ServerRpcHandler`
- `UidlWriter`
- `VaadinSession`
- `UI` lookup / lock / access
- CSRF/security key
- sync id / resync

实现一个 desktop `VaadinRequest` / `VaadinResponse` adapter 或等价 SPI，使 Flow event -> listener -> UIDL patch 可以脱离真实 HTTP socket。

如果必须修改或 fork Vaadin 内部逻辑，先在仓库内做最小 adapter/fork spike，不直接修改 upstream Vaadin 源码，除非另行确认。

### 3. IPC Protocol v1

从 Host Protocol v0 演进为 Flow IPC Protocol v1。

协议必须覆盖：

- `launch`
- `ready`
- `bootstrapRequest`
- `bootstrapResponse`
- `event`
- `patch`
- `command`
- `capabilityResult`
- `push`
- `resync`
- `error`
- `shutdown`

envelope 至少包含：

- `type`
- `id`
- `correlationId`
- `sessionId`
- `uiId`
- `route`
- `payload`
- `error`
- `timestamp`

协议必须有版本号、错误模型、trace 格式和兼容策略。

提供 `docs/ipc-protocol-v1.md` 和真实 trace 示例。

### 4. Swift/AppKit Native Host

继续以 `vaadin-desktop-host-swift` 作为 macOS native host 主线。

Host 负责：

- 启动 Java sidecar
- 建立 IPC transport
- 打开 AppKit `NSWindow`
- 创建 `WKWebView`
- 加载本地 Flow client resources
- 注入 WebView bridge
- 转发 WebView event 到 Java Flow runtime
- 接收 Java patch 并应用到 WebView
- 管理窗口关闭、sidecar 清理、异常退出

不再把最终路径定义为 `WKWebView.load(http://127.0.0.1:port)`。

如需加载静态资源，应优先使用 app bundle、本地文件、自定义 scheme 或 `WKURLSchemeHandler`，而不是 localhost HTTP server。

### 5. WebView Bridge / Flow Client Engine

建立 WebView 内的 JS bridge，使 Vaadin Flow client engine 可以：

- 请求 bootstrap
- 发送 client events
- 接收 UIDL/patch
- 触发 resync
- 上报错误

尽量复用 Vaadin Flow client engine，不重新实现完整客户端。

明确记录需要 shim、patch 或复用的 Flow client 部分。

验证 Button click、Grid/List 数据展示、状态更新不走 HTTP。

### 6. Native Capability Gateway

将当前 desktop API scaffold 演进为真实 native gateway 初稿。

至少实现并验证：

- window title
- notification 或 clipboard 二选一
- file dialog 可先保留 scaffold，但接口要稳定

Capability 调用必须走 IPC command/capabilityResult。

文档中明确 capability 权限模型的最小设计，即使 v1 只实现 allow-all dev mode。

### 7. 真实测试应用

继续使用 `test-apps/vaadin-desktop-smoke-app` 作为独立测试工程。

新增或改造一个最终验证 app，例如：

- `test-apps/flow-over-ipc-smoke-app`

该测试工程必须像普通开发者项目一样依赖框架模块。

测试工程必须包含真实 Vaadin Flow View：

- `@Route("")`
- Button click listener
- Span/Label 状态更新
- Grid/List 数据展示
- 至少一个 desktop runtime API 调用

该测试工程的核心 UI 闭环必须不依赖 localhost HTTP server。

### 8. 开发命令

新增明确命令，例如：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`dev-ipc` 必须启动最终 IPC runtime。

`dev` 或 `dev-swift` 可以保留为 compatibility mode，但命名、README 和日志必须清楚区分。

启动日志必须直接显示当前 transport：

- `transport=flow-ipc`
- `compatibilityMode=false`
- 或失败时明确说明为何 fallback 到 compatibility mode

### 9. 可观测性和调试

提供完整 IPC trace：

- bootstrap request/response
- event
- patch
- command
- capabilityResult
- push/resync
- error
- shutdown

trace 必须能关联一次 Button click 的完整链路：

- WebView event
- IPC event envelope
- Java listener
- UIDL/patch generation
- IPC patch envelope
- WebView patch applied

提供最小 inspector 或 trace replay/readme，能让工程师 review 问题。

### 10. 文档更新

新增或更新：

- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `AGENTS.md`

文档必须明确：

- 当前最终 IPC runtime 如何工作
- 哪些 Flow 能力已经脱离 HTTP
- 哪些能力仍在 compatibility fallback
- 为什么这个方案不是 WebView 套壳
- Swift/AppKit host、WKWebView、Java sidecar、Flow runtime 的责任边界
- 后续如何推进 push、upload/download、packaging、permission、upstream Vaadin SPI

## 验收标准

1. 仓库中存在可运行的 Flow over IPC runtime 路径。
2. 有明确命令可以启动 IPC 模式，例如 `dev-ipc`。
3. 至少一个真实独立测试工程可以在真实 macOS 桌面窗口中运行。
4. 测试工程使用真实 Vaadin Flow API 编写 UI。
5. Button click -> Java listener -> UI state change -> UIDL/patch -> WebView update 的闭环不依赖 localhost HTTP server。
6. Grid/List 数据展示路径不依赖 localhost HTTP server。
7. 至少一个 desktop runtime API 调用走 IPC command/capabilityResult。
8. trace 中能完整看到 bootstrap、event、patch、command、capabilityResult、shutdown。
9. 真实窗口通过 WindowServer、截图或等价方式验证，不只依赖单元测试或日志。
10. compatibility mode 仍可保留，但不能被作为最终 Flow over IPC 验收路径。
11. 所有未完成的 Flow 能力必须明确列出，例如 push、upload/download、resource loading、dev mode、production bundle。
12. 文档足够让工程团队继续拆 issue，而不是只留下实验代码。

## 非目标

- 不要求一次完成三平台实现。
- 不要求生产级签名、自动更新、安装器。
- 不要求完整权限系统。
- 不要求覆盖所有 Vaadin 组件和高级能力。
- 不要求立刻 upstream 到 Vaadin Flow。
- 不要求删除 compatibility mode。
- 不允许把 localhost HTTP WebView 路径伪装成最终 Flow over IPC。

## 停止条件

当前仓库形成一个可运行、可评审的 **Vaadin Flow over IPC runtime v1**：

- 工程师可以用 `dev-ipc` 启动一个真实 Vaadin Flow desktop app。
- 真实 macOS native window 中的 Button、状态更新、Grid/List 和至少一个 desktop capability 调用都通过 IPC runtime 完成。
- 核心 UI 交互路径不依赖 localhost HTTP/Servlet/WebSocket。

同时，仓库必须包含完整协议文档、Flow adapter 文档、native host 文档、trace 示例、独立测试工程和明确的 compatibility fallback 边界，使下一阶段可以继续推进 push、resource loading、upload/download、packaging、permission model 和可能的 Vaadin upstream SPI。

## 关键约束

本 goal 的关键验收约束是：**Button click 到 UI patch 回 WebView 的核心闭环不能依赖 localhost HTTP server**。

这会强制实现真正触达 Vaadin Flow transport，而不是继续只增强 native host 外壳。
