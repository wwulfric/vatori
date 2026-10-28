# vatori 产品功能目标

本文档定义 `vatori` 当前稳定的产品功能目标。它应保持精炼、长期有效，
不作为逐轮实现日志使用。

## 当前范围

当前目标是在 macOS 桌面运行时上实现产品级 Vaadin Flow 功能。应用继续使用标准
Vaadin Flow Java 编程模型、标准 Vaadin production bundle，以及官方 Flow
client/server。Native host 负责 WKWebView、生命周期和原生能力；opaque IPC
transport 负责承载 Flow request、push、event、patch 和 capability
command/result，但不解释组件或 UIDL 语义。

当前目标包含四类产品能力：

1. **Flow 功能**：共享资源加载、浏览器事件、Java listener、UIDL 状态同步、
   overlay、焦点、键盘行为、无障碍、Grid/lazy-data 行为和 Upload/Download
   streaming 均通过规范的 Flow IPC 主链工作。
2. **真实桌面交互**：代表性组件族在真实 WKWebView 中具备可信的原生输入、焦点和
   无障碍证据，同时覆盖无法只靠浏览器 fixture 建立的外部桌面交互边界。
3. **运行时连续性与耐久性**：session-scoped push、reconnect、resync、
   multi-window、sidecar replacement，以及长时间或并发传输都有明确且有界的结果，
   不会造成跨 session 或跨窗口污染。
4. **macOS 原生产品能力**：公开的 clipboard、dialog、notification、window 和
   file API 具备完整的权限模型和 sandbox-safe 文件授权，并在适用场景下提供可关联的
   lifecycle、progress、cancellation、retry 和 resume 行为。

## 完成标准

只有同时满足以下条件，才能认为产品功能目标完成：

- 共享能力矩阵已覆盖代表性的组件族和行为族，不存在已知的组件专用 production
  例外；
- 所声明的 Flow 行为均在真实 macOS WKWebView 中，通过标准 production bundle、
  官方 Flow client/server 和 opaque IPC 得到验证；
- 相关浏览器事件、Java listener、UIDL 或 push update、可见状态，以及 native
  command/result 均具有端到端覆盖；
- 原生能力的权限、授权和生命周期职责达到产品安全要求，而不是依赖验收 fixture
  约定；
- framework、IPC 和 native-host production code 中不存在 fixture id、组件名称、
  自动化逻辑或组件语义协议分支；
- 所有仍依赖 focused validation、browser-programmatic 操作或 compatibility
  transport 的能力，要么已经闭合，要么被明确排除在所声明的产品能力面之外。

一个组件 fixture 只能闭合它所证明的通用能力 slice。单纯增加 fixture 数量，不能证明
全面产品功能 parity 已经完成。

## 稳定架构边界

- Compatibility mode 必须保持显式。Servlet filter、servlet-backed security 和
  常规 WebSocket infrastructure 属于 Spring Boot compatibility adapter，不能计入
  pure Flow IPC parity。
- 规范的 Flow IPC 主链不得引入自定义 Flow client、UIDL interpreter、组件 renderer
  或 native-host 组件适配器。
- 修改 production code 前必须先分类失败：可复用 framework/runtime 缺口、macOS
  native-host 产品边界、上游/应用语义，或仅验收覆盖缺口。只有可复用的产品能力才允许
  修改 production code。
- Examples 必须保持开发者视角的外部应用形态。确定性 hook、组件 id、自动化、故障注入
  和 trace gate 只能放在 `test-apps`、`test-support` 或测试源码中。

## 独立 Roadmap 话题

以下内容具有价值，但不阻塞当前产品功能目标完成：

- 开发体验，包括 JVM 原地 class 热替换、完整上游 Vaadin DevTools 集成，以及更广的
  开发源码观察；
- 分发和更新交付，包括 embedded JRE、installer、production signing/notarization、
  release automation 和 auto-update；
- Windows 和 Linux native host。它们属于更远期的跨平台方向，不在当前 macOS
  目标内。

这些内容继续以 TODO 或远期方向的形式记录在 `docs/roadmap.md` 中，不得与产品功能
parity 状态混在一起。

## 长期状态文档

当前实现状态和验收证据维护在：

- `docs/flow-over-ipc-runtime.md`；
- `docs/acceptance-tools.md`；
- `docs/roadmap.md`；
- 需要长期保留架构决策时使用的 focused architecture document。

## 执行规则

每轮只推进一个最小、通用、可验证的产品功能 slice。不要创建 `docs/goal-*.md` 等逐轮
目标快照或归档目录。长期状态变化应更新现有 runtime、acceptance 和 roadmap 文档；
单轮运行证据由 git history、CI artifact、release asset 和最终汇报承载。
