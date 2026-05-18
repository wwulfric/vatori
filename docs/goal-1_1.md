# Goal 1.1: Vaadin Flow over IPC Runtime Hardening

`docs/goal.md` 可视为 **goal-1_0**：它已经把仓库推进到一个可运行、可评审的
Vaadin Flow over IPC runtime v1。当前 v1.0 路径已经能用 `dev-ipc` 启动真实独立
Flow app，在真实 macOS Swift/AppKit/WKWebView 窗口中完成无 localhost HTTP 的
bootstrap、Button click、UIDL patch、List/Span 展示、最小 push、verification resync、
desktop command/capabilityResult 和 resource graph request/response 证据。

本阶段目标不是重新证明 v1.0，也不是回到 localhost compatibility mode；而是在
v1.0 的可评审运行时基础上，把受限 smoke runtime 推进为更接近产品化的
**Flow over IPC runtime v1.1**。

核心方向：

- 扩大真实 Vaadin Flow client engine 能力覆盖。
- 让前端资源加载从“资源图可请求/验证”推进到“更多资源可按需执行/解析”。
- 把 push/resync 从单次验证路径推进到可解释的队列、顺序和恢复模型。
- 把 native capability gateway 从 scaffold/allow-all-dev 推进到更真实的主机能力和权限边界。
- 增加上传/下载、文件选择、错误恢复和 trace inspector 证据。

## 继承边界

### 已完成的 v1.0 能力

以下能力在 v1.1 中视为基线，不能回退：

- `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app`
  启动 no-localhost Flow IPC runtime。
- Swift/AppKit host 用 `--transport flow-ipc` 打开本地 WKWebView shell。
- Java sidecar 与 host/WebView 通过 `VFIPC1` stdin/stdout envelope 通信。
- `bootstrapRequest` / `bootstrapResponse` / `event` / `patch` / `push` /
  `resync` / `command` / `capabilityResult` / `error` / `shutdown` 已存在。
- Bootstrap 和最小 push 使用真实 Vaadin `UidlWriter`。
- Click event 和 verification resync 使用真实 `UidlRequestHandler` /
  `ServerRpcHandler` 路径。
- WebView 中可见 Flow client engine 消费 bootstrap、click patch、push 和 resync UIDL。
- `FlowBootstrap.js` / `FlowClient.js` 通过 IPC 加载、导入和初始化。
- 当前 advertised Flow client classpath resources 全部能被 WebView 请求和验证。
- `DesktopTraceInspector --require-flow-ipc-v1` 能 gate v1.0 必要证据。
- compatibility mode 明确保留为 `dev` / `dev-swift`，不能伪装成 Flow IPC。

### 本阶段关键约束

- v1.1 验收路径仍然不能依赖 `WKWebView.load(http://127.0.0.1:port)`。
- 核心 UI transport 不能回退到 Servlet/HTTP/WebSocket compatibility mode。
- 应用开发者仍然编写普通 Vaadin Flow Java View，不手写 IPC。
- 不直接修改本地 upstream Vaadin 源码；如需 fork/spike，先放在本仓库。
- 保留 v1.0 inspector fixture 和真实窗口验证路径，并扩展而不是弱化它们。
- 任何“不完整”能力必须在文档和 trace 中显式标记，不允许把 scaffold 描述成 final。

## 实现范围

### 1. Flow Client Resource Execution

v1.0 已经能请求和验证全部 advertised classpath resources，但只执行
`FlowBootstrap.js` 和 augmented `FlowClient.js`。v1.1 要把资源路径推进到更真实的
Flow frontend runtime。

目标：

- 为 WebView 建立明确的本地资源解析策略，优先考虑：
  - `WKURLSchemeHandler`
  - `flow-ipc://...` custom scheme
  - app bundle/local file staging
  - IPC resource request fallback
- 让 Flow client engine 的 lazy/dynamic resource request 可以被 host/Java 解析。
- 覆盖 `Flow.loadOnDemand(...)`、bootstrap `LAZY` entries、theme/static resource 读取。
- 区分并记录：
  - graph requested
  - graph verified
  - resource executed
  - resource intentionally not executed
  - execution failed
- 为 production bundle 与 dev mode 分别记录当前支持边界。

验收证据：

- trace 中能看到 lazy/frontend resource lookup，不只是 manifest graph。
- `webview.dom` 或 inspector 输出能区分 executed resources 与 verified-only resources。
- sample trace 更新为 v1.1 fixture。
- 文档明确哪些资源已经执行、哪些仍未执行，以及为什么。

### 2. Broader Flow Component And Event Coverage

v1.0 的核心闭环主要覆盖 Button click、Span/List 展示和状态更新。v1.1 要验证更多
真实 Flow component/event 形态仍能通过 visible Flow client engine 与 server-side
Flow RPC 路径闭环。

优先覆盖：

- Text input value change，例如 `TextField` 或等价输入组件。
- Boolean event，例如 `Checkbox`。
- Selection event，例如 `Select`、Radio group 或最小可行替代。
- Repeated item/list update，例如 `Grid`、VirtualList 或明确标记的 Flow List fallback。
- Component attach/detach 或 enable/disable 状态变化。

要求：

- 事件必须由 Flow client engine 产生 Flow-shaped RPC/XHR payload。
- Java listener 必须通过真实 Flow event/RPC 路径触发。
- UI patch 必须由 server-side Flow UIDL 路径返回并被 visible Flow client engine 消费。
- 如某个组件因 resource/dev-mode 缺口暂不能完成，必须保留失败分析和下一步拆解。

验收证据：

- 新增或扩展 `test-apps/flow-over-ipc-smoke-app`，包含 v1.1 component/event matrix。
- trace inspector gate 至少两个非 Button event 的完整 IPC -> listener -> UIDL patch 链路。
- `webview.dom` 记录每类组件的 rendered state 和 post-event state。

### 3. Push, Ordering, Reconnect And Resync Semantics

v1.0 只验证了一个早到 push 被 WebView queue 后交给 Flow client engine。v1.1 要建立
可评审的最小顺序和恢复模型。

目标：

- Java-to-WebView messages 保持 monotonic `serverSequence`。
- WebView 对早到、乱序、重复和延迟 patch/push 有明确处理策略。
- 建立最小 ack 或 observed-sequence 记录，用于判断是否需要 resync。
- resync 不只是 verification hook，应能作为恢复路径被触发和验证。
- sidecar/host window close、Java crash、WebView reload 至少有 trace-level 恢复或失败语义。

验收证据：

- 新增 focused smoke 或 inspector fixture，覆盖：
  - multiple push messages
  - queued then flushed order
  - duplicate/late message handling
  - explicit resync request and response
- `DesktopTraceInspector` 新增 v1.1 gates，而不是只检查 v1.0 的 single `push:3`。
- 文档定义 v1.1 push/resync 的保证和非保证。

### 4. Native Capability Gateway v1.1

v1.0 已有 `window.setTitle`、native clipboard write、trace-only notification、
manifest-backed allow-all-dev decision metadata，以及 cancelled `fileDialog.openFile`
scaffold。v1.1 要把 native gateway 推进到可用的主机能力初稿。

目标：

- 用 AppKit `NSOpenPanel` 实现真实 `fileDialog.openFile`。
- 返回稳定 typed result：
  - `success`
  - `canceled`
  - selected path(s) 或 security-scoped descriptor
  - permission metadata
  - correlation id
- 保持 Java API 开发者体验稳定，例如 `Desktop.current().fileDialog().openFile(...)`。
- 实现至少一个 deny-path smoke：
  - 未声明 command 被拒绝
  - unsupported command 被拒绝
  - manifest 缺失或 parse error 的行为明确
- 将 `permissionMode=allow-all-dev` 推进到可切换模式：
  - dev allow mode
  - manifest-enforced mode
  - trace-only dry-run mode，若实现成本更低

验收证据：

- 真实 macOS 窗口中可触发 `NSOpenPanel`，自动化可使用取消路径验证。
- trace 中包含 file dialog request/result、permission decision 和 cancellation/result metadata。
- inspector gate 覆盖 real file dialog path 或至少 real native panel cancellation path。
- 文档明确 production permission model 尚未完成的部分。

### 5. Upload And Download Bridge

v1.0 明确未实现 upload/download。v1.1 要实现最小桌面文件流边界，不要求完整
Vaadin Upload parity，但必须有真实可评审路径。

优先目标：

- Download：
  - Java sidecar 发起 download command 或 resource result。
  - Swift host 使用 `NSSavePanel` 或受控 test path 写文件。
  - result 通过 `capabilityResult` 或 dedicated IPC result 返回。
- Upload：
  - host file dialog 选择文件。
  - 将 file descriptor/path/token 传回 Java sidecar。
  - Java runtime 能读取或模拟读取，并更新 Flow UI 状态。

约束：

- 不把文件内容通过未界定的大 JSON body 盲目塞进 trace。
- trace 中要 redacted body，并记录 byte count、mime/type、filename、result status。
- 明确 security-scoped bookmark / sandbox 还未完成或已完成的范围。

验收证据：

- 独立 smoke path 验证至少一个 download 和一个 upload/select-file 结果。
- 真实窗口或可重复 headless host simulation 能验证文件 result。
- docs 说明 upload/download 与 Vaadin 原生 Upload/Anchor/StreamResource 的差距。

### 6. IPC Protocol v1.1

在 v1 基础上定义 v1.1 扩展，不破坏 v1.0 fixture。

必须覆盖或扩展：

- resource execution/lazy lookup metadata
- multi-push ordering metadata
- upload/download command/result metadata
- real file dialog result metadata
- explicit permission decision model
- error categories：
  - protocol error
  - security/scope rejection
  - resource missing
  - capability denied
  - native host failure
  - Java runtime failure
- backward compatibility notes from v1.0 to v1.1。

交付：

- 更新 `docs/ipc-protocol-v1.md`，或新增 `docs/ipc-protocol-v1_1.md`。
- 新增 `docs/traces/flow-ipc-v1_1.ndjson`，保留 v1.0 trace fixture。
- Inspector 支持 `--require-flow-ipc-v1_1` 或等价 strict gate。

### 7. Verification App And Tooling

继续保持 app 是普通开发者项目形态。

允许两种路线：

- 扩展 `test-apps/flow-over-ipc-smoke-app` 为 v1.1 smoke。
- 新增 `test-apps/flow-over-ipc-v1_1-smoke-app`，如果避免污染 v1.0 fixture 更清晰。

测试工程必须包含：

- `@Route("")`
- Button
- at least two non-Button client events
- repeated item/list/grid data display
- push/resync visible state
- file dialog API call
- upload/download smoke UI
- permission denied or unsupported capability test hook

命令要求：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

可新增：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-v1_1-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
```

### 8. Documentation

更新或新增：

- `docs/flow-over-ipc-runtime.md`
- `docs/ipc-protocol-v1.md` 或 `docs/ipc-protocol-v1_1.md`
- `docs/flow-transport-adapter.md`
- `docs/native-host.md`
- `docs/framework-architecture.md`
- `docs/getting-started.md`
- `docs/roadmap.md`
- `README.md`
- `AGENTS.md`

文档必须明确：

- v1.0 与 v1.1 的差异。
- 哪些能力仍属于 compatibility mode。
- 哪些能力通过 Flow IPC runtime 完成。
- 哪些能力只是 scaffold、trace-only 或 verified-only。
- Native host、WebView bridge、Java runtime、Flow adapter 的责任边界。
- 下一阶段如何继续推进：
  - full Flow frontend execution
  - production permission model
  - packaging/signing/updater
  - cross-platform hosts
  - upstream Vaadin SPI proposal

## 验收标准

1. v1.0 `dev-ipc` smoke 继续通过，且不回退到 localhost HTTP。
2. v1.1 smoke app 或扩展 smoke app 能在真实 macOS native window 中运行。
3. 至少两个非 Button Flow client events 完整走 IPC -> Java listener -> UIDL patch -> visible Flow client update。
4. Repeated item/list/grid 数据展示和至少一次数据变更通过 Flow IPC runtime 验证。
5. 多个 server-initiated push 或 push-like updates 按可解释顺序被 queue/flush/consume。
6. Resync 作为恢复路径被验证，不只是一次 happy-path hook。
7. Flow client resource path 覆盖 lazy/resource lookup，并区分 verified-only 与 executed。
8. `fileDialog.openFile` 使用真实 AppKit panel 或真实 native cancellation path，不再只是 synthetic scaffold。
9. 至少一个 upload/select-file 和一个 download/save-file 边界被实现或以明确可评审 scaffold 验证。
10. Capability permission 至少有 manifest-enforced deny-path 证据。
11. IPC trace fixture 覆盖 v1.1 新消息/metadata，inspector 有 strict gate。
12. 文档清楚说明 v1.1 已完成能力、仍未完成能力和 compatibility fallback。
13. 真实窗口验证包含 `webview.dom`、`webview.snapshot` 或等价 WindowServer/screenshot 证据。
14. 退出后 Java sidecar 和 native host 没有残留进程。

## 非目标

- 不要求一次完成 Windows/Linux host。
- 不要求生产级安装器、签名、公证、自动更新。
- 不要求完整 Vaadin component parity。
- 不要求完整 Vaadin Upload/Download API parity。
- 不要求完整生产权限系统。
- 不要求立即 upstream 到 Vaadin Flow。
- 不要求删除 v0/v1 compatibility mode。
- 不允许把 localhost HTTP path 作为 v1.1 验收路径。

## 停止条件

当前仓库形成一个可运行、可评审的 **Vaadin Flow over IPC runtime v1.1**：

- 工程师可以用明确命令启动真实 macOS native window。
- 真实 Flow app 的 Button、至少两个非 Button event、repeated data display、push/resync、
  file dialog、upload/download 边界和 permission deny-path 都有 IPC trace 证据。
- 核心 UI 交互路径、resource lookup 证据和 native capability gateway 不依赖 localhost HTTP。
- v1.0 trace/inspector 继续通过，v1.1 trace/inspector 新增并通过。
- 文档足以让下一阶段继续拆 production frontend execution、packaging/updater、
  production permission model、cross-platform host 和 upstream Vaadin SPI。

## 关键约束

本 goal 的关键验收约束是：

**v1.1 只能在 v1.0 no-localhost Flow IPC runtime 之上扩展能力，不能通过恢复 Servlet/HTTP/WebSocket compatibility transport 来满足验收。**
