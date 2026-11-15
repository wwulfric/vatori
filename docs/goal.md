# vatori 产品功能目标

本文档定义 `vatori` 当前稳定的产品功能目标。它应保持精炼、长期有效，
不作为逐轮实现日志使用。

## 当前阶段：首个 macOS 产品功能基线

阶段 A 的能力矩阵、量化基线和延期项已获用户确认；当前进入阶段 B 实施和复验。
唯一能力清单及负载边界见 runtime 文档的 Product Capability Matrix 与 First
Acceptance Baseline；证据规则见 acceptance 文档，依赖顺序见 roadmap。

1. 完成所有 B 项及 B/E 的基线部分；N06 等明确延期项保留为长期目标，不阻塞本基线。
2. 优先解决窗口 active/key 激活和验收证据覆盖，再推进生命周期与产品 replacement、
   共享 Flow 行为、真实系统交互和并发耐久性。不能降低门禁来掩盖失败。
3. 按共享契约而非组件排列组合推进，先分类缺口；实现状态与验证状态分开维护。
   保留未知、证据缺失和当前回归失败，历史通过或 synthetic 测试不能替代当前真实证据。
4. 同一源码 revision、可追溯 bundle 和明确运行环境下，基线必选验收全部通过，且无
   范围内已知缺陷、证据缺失或 production 测试逻辑泄漏，才可宣布本基线完成。
5. OS 权限、剪贴板和外部操作按具体行为取得授权，不复用已经消费的一次性授权。

基线冻结后，范围内已知缺陷必须修复；新增独立能力须显式调整范围。完成时只声明
“首个 macOS 产品功能基线完成”，不声明长期全面 parity。阶段 C 再扩展规模、组合
和明确延期的产品契约。

## 长期产品范围

长期目标是在 macOS 桌面运行时上实现产品级 Vaadin Flow 功能。应用继续使用标准
Vaadin Flow Java 编程模型、标准 Vaadin production bundle，以及官方 Flow
client/server。Native host 负责 WKWebView、生命周期和原生能力；opaque IPC
transport 负责承载 Flow request、push、event、patch 和 capability
command/result，但不解释组件或 UIDL 语义。

长期目标包含四类产品能力：

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

## 长期产品完成标准

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

当前执行契约是有限阶段 B，而不是不断扩展直到长期全面 parity。一次范围核对的结果
维护在 runtime 文档的“阶段 B 有限收口清单”；它是现有矩阵的执行状态，不是第二份
能力范围。每项归为已有符合基线的真实证据、已实现但缺必要证据、已知产品缺陷或
基线外延期项；缺口必须有最小证明方法和明确关闭条件。

1. 一次任务交付一个完整开发者行为或基线场景。开工先固定关闭项及通过条件；fixture、
   驱动、runner、必要断言、真实运行和结果核验是内部步骤，不得在没有实际阻塞时
   分拆成连续的 helper 交付。连续两轮只有验收基础设施变化时，必须检查方向、指出
   它阻塞的具体基线项，并合并完成剩余链路，不自然延伸成验证器工程。
2. 基线场景通过即标记“基线场景已满足，待最终统一构建复验”，停止主动加固。
   只有新发现的范围内产品缺陷或相关行为变化才重新打开。更多组合、规模与独立能力
   进入 backlog；不得自动成为完成阻塞项，也不得以收敛为由降低已批准标准。
3. 状态机与竞态组合优先用确定性测试；真实 WKWebView 证明批准的代表性端到端行为。
   完整 framework/全基线回归放在相关集成节点及最终收口，不为每个窄断言重跑无关场景。
4. 验证器修改优先重放保留的真实 trace，分别记录原运行状态和新核验结果。仅当必要
   采集信息缺失、驱动/运行条件或产品行为变化时新增真实运行；不改写失败证据。
5. 只调整立即解除交付阻塞的 harness，不另开测试平台重构。优先复用同一可追溯 bundle
   执行多个隔离场景，保留权限隔离、进程退出确认、generation fencing 和独立证据。
6. 文档仅在稳定能力状态变化时更新；不为每个 helper 同步多份长文档。不创建
   `docs/goal-*.md`、目标快照或过程归档。保留未提交工作，单次结果留在最终汇报、
   git history 或独立生成证据中。

完成必选项、最终统一构建复验和生产边界检查后，仅宣布“首个 macOS 产品功能基线完成”。
内建 goal objective 若不能原地修改，以用户修订指令及本文为有效执行契约；不得通过
将未完成目标标为 complete 来替换目标或恢复执行。
