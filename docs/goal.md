# Goal: Vaadin Desktop Framework Long-Term Vision

本文件是仓库的长期总目标，用于持续跟踪 `Vaadin Desktop Framework` 从当前
v0/v1 runtime 切片走向可产品化桌面框架的最终完成状态。

历史阶段目标保留为不可变的阶段快照：

- `docs/goal-1_0.md`：Flow over IPC Runtime v1，已完成。
- `docs/goal-1_1.md`：Flow IPC runtime hardening，已完成。
- `docs/goal-1_2.md`：first real Vaadin Web Component over Flow IPC，已完成。
- `docs/goal-1_3.md`：framework-shaped Flow IPC validation app，已完成。
- `docs/goal-1_4.md`：Flow IPC frontend asset pipeline，已完成。
- `docs/goal-1_5.md`：no-localhost frontend bundle staging，已完成。

后续阶段应继续以 `docs/goal-1_6.md`、`docs/goal-1_7.md` 等文件拆分，
每个阶段只拿走本长期目标中的一个可评审切片。不要把本文件改写成短期任务清单；
它是长期北极星和完成度账本。

## 最终愿景

构建一个真实可用的 `Vaadin Desktop Framework`：

- Java 开发者继续使用普通 Vaadin Flow 编程模型，例如 `@Route`、`Button`、
  `Grid`、`FormLayout`、`Upload`、`Dialog`、`Binder`、listeners 和 server-side state。
- 桌面应用通过系统 WebView 渲染，但核心 UI transport 不依赖 localhost
  HTTP/Servlet/WebSocket。
- Flow bootstrap、client event、UIDL patch、server push、resync、resource loading、
  command 和 capability result 都有清晰 IPC contract。
- Swift/AppKit、Windows WebView2 和 Linux WebKitGTK host 管理窗口、菜单、托盘、
  clipboard、dialog、notification、drag/drop、快捷键、生命周期、打包和更新。
- Compatibility mode 明确保留为开发/迁移 fallback，但不伪装成最终架构。
- 框架像 Tauri + Vaadin Flow 的组合：开发者写 Java/Vaadin，框架负责 native host、
  IPC、能力网关、打包和运行时边界。

## 当前基线

截至 goal 1.5，仓库已经证明：

- `dev-ipc` 可以启动 no-localhost Flow IPC runtime。
- 真实 macOS Swift/AppKit/WKWebView 窗口可以运行 Flow IPC smoke app 和 framework
  validation app。
- Button、BigDecimalField、Checkbox、Select、list/repeated update、desktop capability
  baseline 都有 trace gate 或 host-log gate 证据。
- Java sidecar 通过 `UidlWriter`、`UidlRequestHandler`、`ServerRpcHandler`、
  desktop `VaadinRequest` / `VaadinResponse` adapter 切片生成真实 UIDL。
- WebView 中可见 Flow client engine 消费 bootstrap、event patch、push 和 resync。
- Flow client classpath resources、component frontend、theme/static、production bundle、
  dev-mode bundle 和 generated/lazy artifact families 已能通过 IPC manifest 表达和验证。
- v1.5 已建立 no-localhost frontend staging resolver：真实 artifact body 可被 stage 成
  Blob-backed module URL，并以 `resourceStage` trace 记录 staged、executed、failed 和
  strict blocker 状态。
- TextField/npm graph、完整 generated/lazy execution、production bundle execution 和
  dev-mode/Vite execution 仍是明确 blocker，不计入完成。

## 最终完成标准

长期目标完成时，仓库应满足以下条件。

### 1. Flow Over IPC 是默认主线

- `dev-ipc` 或其后继命令是默认推荐的桌面开发路径。
- 核心 UI 闭环不依赖 localhost HTTP/Servlet/WebSocket：
  bootstrap、events、UIDL patches、push、resync 和 resource loading 都通过 IPC。
- Compatibility mode 仍可运行，但文档、日志和 CLI 明确标记为 fallback。
- Runtime 不依赖 smoke-only hooks 才能正常工作。

### 2. Flow Adapter 边界稳定

- `vaadin-desktop-flow` 中有清晰、可维护的 Flow transport adapter。
- `UidlRequestHandler`、`ServerRpcHandler`、`UidlWriter`、`VaadinSession`、
  UI lock/access、CSRF/security key、sync id 和 resync 语义被封装在稳定边界内。
- 不直接修改本地 upstream Vaadin 源码；如需 upstream SPI，先以仓库内 adapter/fork
  证明并记录。
- 错误路径、session lifecycle、multi-window/multi-UI 行为有明确 trace 和测试。

### 3. Frontend Artifact Pipeline 可产品化

- Flow client resources、component frontend modules、theme/static、generated/lazy、
  production bundle 和 dev-mode/Vite resource graph 都有统一 resolver。
- TextField/npm graph 不再依赖旧 shim；真实 TextField、Checkbox、Select 等组件通过
  真实 Vaadin frontend artifact 执行。
- Generated/lazy `Flow.loadOnDemand` path 可以稳定工作，或有可接受的替代 SPI。
- Production bundle / dev-mode bundle 有明确 staging、cache、cleanup 和 integrity 策略。
- WebView resource loading 使用 app bundle、本地 staging、custom scheme 或等价
  no-localhost mechanism。

### 4. 组件和 Flow 功能覆盖足够真实

- 不只覆盖 Button 和少数组件；至少覆盖真实业务应用常见路径：
  TextField、Number/BigDecimalField、Checkbox、Select、ComboBox、DatePicker、
  Dialog、FormLayout、Grid/VirtualList、Upload/Download、theme variants。
- 事件必须经 Flow client engine -> IPC-backed bridge -> Java listener ->
  UIDL patch -> visible DOM 的真实链路。
- Repeated data、selection、validation、enabled/disabled、attach/detach、locale/format、
  focus/keyboard 和 error state 有回归验证。

### 5. Native Host 具备桌面框架能力

- macOS Swift/AppKit host 是稳定主线，并有可重复 smoke tests。
- Windows WebView2 和 Linux WebKitGTK host 至少达到可运行的同级架构切片。
- Host 负责 native 行为：窗口、菜单、托盘、clipboard、file dialog、notification、
  drag/drop、shortcuts、IME/focus、context menu、lifecycle、crash cleanup。
- Window activation、window level、first meaningful paint、blank/flicker recovery、
  close/shutdown 和 sidecar cleanup 有自动化验证。

### 6. Capability Gateway 有权限模型

- Java desktop API 稳定且面向开发者，例如 window、clipboard、notification、
  file dialog、file transfer、shell/open URL、menu/tray。
- IPC `command` / `capabilityResult` typed、async、可追踪，并带 correlation id。
- `desktop-capabilities.json` 或后继 manifest 支持 allow、deny、trace-only、dev override
  和 production policy。
- 文件能力支持受控路径、真实 native panel、security-scoped metadata、redacted trace、
  byte count 和错误分类。

### 7. CLI、打包和开发体验完整

- CLI 能创建、运行、调试、打包和检查桌面应用。
- Maven/Gradle 插件或等价工具能处理 framework dependencies、frontend bundle staging、
  native host assets、capability manifest 和 release metadata。
- 开发者项目像普通外部项目一样依赖 framework modules，不需要复制 test-app 特殊代码。
- Trace inspector、doctor、resource graph inspector 和 failure diagnostics 足够让工程师
  快速拆 issue。

### 8. 分发和更新路径可落地

- macOS 至少支持 app bundle、签名/公证规划、DMG/PKG 或等价分发。
- Windows/Linux 有清楚打包边界和后续实现计划。
- Java sidecar、native host、frontend artifacts、capability manifest 和 updater metadata
  有一致的发布结构。
- 自动更新可以先是设计和 skeleton，但最终需要可验证的安全更新路径。

### 9. 文档和验证体系完整

- README、getting started、architecture、native host、Flow adapter、IPC protocol、
  runtime limitation、roadmap、troubleshooting 都保持同步。
- 每个阶段都有 fixture trace、strict inspector gate、真实窗口验证命令和 commit。
- 文档明确已完成能力、compatibility fallback、partial bridge、diagnostic shim、
  strict blocker 和下一步拆解。
- 不把实验路径包装成 final，不把 localhost compatibility mode 包装成 Flow over IPC。

## 长期未完成账本

以下项目仍需要后续阶段推进：

- 替换 v1.5 中剩余的 TextField/npm shim fallback，完成真实 TextField graph parity。
- 完整 generated/lazy `Flow.loadOnDemand` execution。
- Production bundle chunk execution 和 packaging-time staging。
- Dev-mode/Vite graph execution 或可靠替代模式。
- Broader Vaadin component parity，尤其是 Grid/VirtualList、ComboBox、DatePicker、
  Dialog、Upload。
- Durable push queue、ack、reconnect、reload 和 resync 语义。
- Multi-window / multi-UI / multi-session lifecycle。
- Host-side smoke tests：window launch、activation、sidecar cleanup、capability calls。
- macOS host 默认化、Windows/Linux host 切片、打包和更新。
- Production permission model 和 security-scoped file access。
- Upstream Vaadin SPI 评估。

## 后续阶段制定规则

每个新的 `goal-1_x.md` 应满足：

- 从本文件的未完成账本中选择一个可评审切片。
- 明确继承哪些已完成阶段能力，且不得回退。
- 明确不使用 localhost compatibility mode 作为 Flow IPC 成功证据。
- 明确验收命令、fixture trace、真实窗口验证和文档更新范围。
- 如果选择 blocker 化路径，必须给出 source artifact、runtime layer、browser/native error、
  failure classification 和下一步拆解。
- 每轮工作按适合粒度提交 Conventional Commit。

## 当前建议的下一阶段

下一阶段应优先选择以下方向之一：

1. **Goal 1.6: TextField / npm graph parity**
   - 目标是替换或严格拆解 `@vaadin/text-field` 剩余 shim，推进真实 TextField frontend
     graph staged execution。
2. **Goal 1.6: Generated/lazy loadOnDemand execution**
   - 目标是把 v1.5 的 generated/lazy staged strict blocker 推进到可执行 lazy path。
3. **Goal 1.6: Durable push / reconnect / resync**
   - 目标是把 duplicate/late probe 扩展为 durable queue、ack、reconnect 和 resync 语义。
4. **Goal 1.6: Host tests and default host readiness**
   - 目标是为 Swift host window launch、no-activate、sidecar cleanup 和 native capability
     增加可重复 smoke tests，为默认 host 做准备。

短期建议优先做 TextField/npm graph parity；它直接拆掉 v1.5 最刺眼的 frontend blocker，
也能给 production bundle / generated lazy 后续工作提供更硬的资源解析证据。
