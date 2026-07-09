---
name: postmortem
description: Analyze vatori fix commits between a git tag/ref and HEAD or another ref, review existing postmortem topics, select representative incidents, and create or append Chinese postmortem reports in ./postmortem/. Trigger for postmortem, 复盘, 事故复盘, incident report, RCA, root cause analysis, analyze fixes, what broke since release, release regression review, /postmortem, or when vatori Flow-over-IPC, Swift host, UIDL adapter, trace gate, CLI/dev workflow, compatibility-mode, or desktop runtime fixes should be turned into durable engineering knowledge.
---

# Postmortem 技能

为 vatori 分析指定 git 范围内的 `fix:` / `fix(scope):` 提交，结合现有 `./postmortem/` 知识库，筛选值得沉淀的代表性事故，并生成或追加中文复盘报告。

Postmortem 是项目知识库，不是提交流水账。默认记录能暴露新根因、架构边界、验收缺口或可复用预防措施的事故；重复修补、纯测试小修、机械修复和同根因 follow-up 应合并、引用或跳过。

## 复盘写作边界

复盘报告只写与事故判断直接相关的项目事实，不在 skill 或报告里复述完整 runtime 状态。遇到 runtime、host 或验收边界时，先查 `docs/flow-over-ipc-runtime.md`、`docs/acceptance-tools.md` 和相关代码，再按以下原则落笔：

- 明确区分 compatibility mode、当前 `dev-ipc` 切片和最终 Flow over IPC 目标；不要把局部验证写成完整能力完成。
- 只声称代码、文档和验收证据已经证明的能力；未完成或未验证的能力写成限制、风险或后续行动。
- 将事故知识写入 `./postmortem/`；只有稳定入口、长期约束或公开状态真的变化时，才建议修改顶层文档。
- 把 trace 当作证据来源，引用 gate、关键事件和路径；不要要求提交完整生成 trace，除非用户明确需要归档。

## 快速用法

```bash
# 分析 v0.1.5 以来的代表性 fix
/postmortem v0.1.5

# 分析最近 tag 以来的代表性 fix
/postmortem latest

# 分析两个 ref/tag 之间的 fix
/postmortem v0.1.4 v0.1.6
```

## 工作流

### 1. 建立项目上下文

先读取：

1. `AGENTS.md`
2. 与候选 fix 相关的文档：
   - Flow IPC runtime：`docs/flow-over-ipc-runtime.md`
   - 验收工具和 trace gate：`docs/acceptance-tools.md`
   - 架构边界：`docs/framework-architecture.md`
   - adapter 边界：`docs/flow-transport-adapter.md`
   - native host：`docs/native-host.md`
   - 路线图：`docs/roadmap.md`
3. 如果涉及 trace gate，读取 `vaadin-desktop-devtools/src/main/java/dev/vatori/desktop/devtools/DesktopTraceInspector.java` 中的当前 gate 定义。
4. 如果涉及 Vaadin Flow 或 Tauri 类比，优先使用本机只读源码：
   - Vaadin：`/Users/bytedance/IdeaProjects/study/vaadin`
   - Tauri：`/Users/bytedance/RustroverProjects/study/tauri`

检查工作区：

```bash
git status --short
git tag --list
```

如果已有未提交改动，不要回滚或覆盖。只编辑 `./postmortem/`，并在最终回复中说明工作区原本不干净。

### 2. 解析范围并发现 fix 提交

范围规则：

| 用户输入 | 范围 |
|----------|------|
| 单个 tag/ref，如 `v0.1.5` | `v0.1.5..HEAD` |
| `latest` | `$(git describe --tags --abbrev=0)..HEAD` |
| 两个 tag/ref，如 `v0.1.4 v0.1.6` | `v0.1.4..v0.1.6` |

验证 ref 存在：

```bash
git rev-parse --verify <ref>
```

提取 fix 提交：

```bash
git log <range> \
  --no-merges \
  --extended-regexp \
  --grep='^fix(\([^)]+\))?!?:' \
  --pretty=format:'%H%x1f%h%x1f%an%x1f%ae%x1f%ai%x1f%s%x1f%b%x1e'
```

对每个 fix 记录：

- 完整 hash、短 hash、作者、邮箱、日期
- subject、body
- scope；无 scope 时从修改文件推断
- issue/PR 引用；不存在本地上下文时只记录引用，不要假装已读取
- 破坏性变更标记：`!` 或 `BREAKING CHANGE:`

### 3. 阅读实际变更

不要只凭提交消息写复盘。对候选提交执行：

```bash
git show <sha> --stat
git show <sha> --
git diff <sha>^..<sha> --name-only
```

定位可能致因提交：

```bash
git log <sha>^ -- <changed-file> --pretty=format:'%h|%an|%ai|%s' --no-merges
git blame <sha>^ -- <changed-file>
```

如果无法确定致因提交，明确写“致因提交无法确定性定位”，并列出候选依据。

### 4. 按 vatori 话题分组

优先匹配已有 `postmortem/NNN-*.md`。如果没有合适话题，再按以下维度分组：

1. Flow-over-IPC runtime：bootstrap、event、patch、ordered push、recovery resync、VFIPC1 envelope、no-localhost shell。
2. UIDL/Flow adapter：`UidlRequestHandler`、`UidlWriter`、`DesktopVaadinRequest`、`DesktopVaadinResponse`、`DesktopFlowRuntimeSession`。
3. Swift/AppKit host：WKWebView load、sidecar launch/cleanup、window lifecycle、Host Protocol trace、VDP1 command envelope。
4. Native capability bridge：file dialog、clipboard、notification、command/result、capabilityResult。
5. Devtools and acceptance：`DesktopTraceInspector`、trace gates、auto smoke hooks、diagnostics false positive/negative。
6. CLI/dev workflow：`bin/vaadin-desktop`、`dev`、`dev-swift`、`dev-ipc`、host selection。
7. Build and resources：Maven modules、offline `.m2`、generated Flow client resources、examples/test-apps as external developer shape。
8. Documentation/runtime boundary：compatibility mode 被误表述、公开状态误导、顶层文档越界。

话题文件命名：

```text
postmortem/NNN-short-topic.md
```

`NNN` 是三位递增编号，`short-topic` 用英文 kebab-case，例如：

- `001-flow-ipc-resync-ordering.md`
- `002-swift-host-sidecar-cleanup.md`
- `003-uidl-adapter-contract-drift.md`

### 5. 候选评审与筛选

写入前必须 review 目标 topic 文件；新话题则 review `postmortem/README.md` 和相似 topic，确认不应追加到已有文件。

对每个候选给出内部决策：

| 候选 | 目标文件 | 决策 | 原因 |
|------|----------|------|------|
| `<sha>` 或 commit group | `postmortem/NNN-topic.md` | include / merge / reference-only / skip | 新根因、重复性、影响面、预防价值 |

通常应写成完整事故组块的情况：

- `dev`、`dev-swift`、`dev-ipc` 或示例应用启动/关闭/交互失败。
- no-localhost IPC 闭环破坏：bootstrap、event、patch、push、resync、command/result 任一关键路径失效。
- Flow/UIDL adapter 合同漂移，可能导致真实 Vaadin component 状态不同步。
- Swift host 生命周期、sidecar cleanup、WKWebView 初始化、窗口关闭或进程回收出错。
- trace gate 或 smoke hook 出现 false positive/false negative，导致回归可能被放过或误判。
- CLI、Maven、离线 repo、examples/test-apps 破坏外部开发者形态。
- 文档把 compatibility mode、当前切片能力或未完成路线图描述错，可能误导架构决策。
- 修复揭示新的系统性缺口，即使影响面较小。

默认跳过或合并：

- 纯格式、拼写、导入顺序、lint。
- 只补同一事故的测试、断言、日志或命名。
- 低影响且无新教训的内部 fixture 修补。
- 已有 topic 完整覆盖相同根因和预防措施的重复案例。

### 6. 严重级别

| 级别 | 标签 | vatori 判定标准 |
|------|------|-----------------|
| SEV-0 | Critical | 数据丢失、安全问题、进程失控，或主要 runtime 对所有用户完全不可用且无 workaround |
| SEV-1 | High | `dev-ipc`/`dev-swift`/核心 Flow integration 在常见路径不可用，关键 release gate 失效，或公开架构边界被严重破坏 |
| SEV-2 | Medium | 核心路径降级但有 workaround，特定 component/host/capability 失败，trace gate 误判，sidecar/window cleanup 可靠性问题 |
| SEV-3 | Low | 窄边缘 case、desktop polish、诊断信息、文档漂移或非关键示例问题 |
| SEV-4 | Trivial | 无用户影响，仅内部测试、fixture、格式或机械修复 |

推断信号：

- 修改核心模块比测试/文档更严重；但 runtime 状态文档误导也可能升到 SEV-2/SEV-3。
- 同一问题 3 个以上 fix、跨多天修复、影响 trace gate 或真实窗口验证时，严重级别上调。
- `crash`、`hang`、`timeout`、`lost event`、`out of order`、`cleanup`、`no localhost`、`contract`、`regression` 等关键词需要重点检查。

### 7. 根因分类

使用项目化分类；必要时可组合：

| 类别 | 描述 |
|------|------|
| Flow Contract Drift | Vaadin Flow/UIDL/client engine 假设与 desktop adapter 不一致 |
| IPC Protocol Drift | envelope、ordering、ack/resync、command/result schema 漂移 |
| Compatibility Leakage | Servlet/HTTP/WebSocket/localhost 假设越过 adapter 或被错误隐藏 |
| Host Lifecycle | Swift/AppKit/WKWebView、sidecar、window、process cleanup 生命周期问题 |
| Resource Management | 进程、文件、trace、端口、stream 或 generated resource 管理不当 |
| Trace/Acceptance Gap | 验收工具缺失、gate 误判、自动 hook 不稳定 |
| Build/ClassPath | Maven module、offline repo、classpath、generated resources、example dependency 问题 |
| Missing Validation | 输入、环境变量、协议字段、状态转换或文档状态缺少校验 |
| Regression | 近期重构或扩展破坏已有行为 |
| Documentation Drift | 文档与 runtime 能力、限制或路线图不一致 |

## 报告格式

所有报告使用中文、专业技术语气和免责文化。聚焦系统、流程和架构边界，不指责个人；作者姓名只在提交表格中出现。

### `postmortem/README.md`

创建或更新索引：

```markdown
# Postmortem 索引

生成日期：YYYY-MM-DD

## 话题列表

| 编号 | 话题 | 最高严重级别 | 组件 | 事故次数 | 最近事故 |
|------|------|--------------|------|----------|----------|
| [001](./001-flow-ipc-resync-ordering.md) | Flow IPC resync ordering | SEV-2 | vaadin-desktop-flow | 1 | YYYY-MM-DD |

## 统计

- **话题总数**：N
- **事故总次数**：N
- **按严重级别**：SEV-0: N, SEV-1: N, SEV-2: N, SEV-3: N, SEV-4: N
- **按组件**：vaadin-desktop-flow: N, vaadin-desktop-host-swift: N, ...
- **按根因**：IPC Protocol Drift: N, Trace/Acceptance Gap: N, ...
- **分析的 fix 提交总数**：N
- **入选事故组块数**：N
- **跳过/合并的 fix 提交数**：N

## 共性问题

- <跨 topic 的重复模式和建议>

## 最近更新

| 日期 | 话题 | 事故 | 变更说明 |
|------|------|------|----------|
| YYYY-MM-DD | [001](./001-flow-ipc-resync-ordering.md) | #1 | 新增事故：... |
```

### Topic 文件

每个 topic 文件必须包含：

```markdown
# <话题标题>

| 字段 | 值 |
|------|-----|
| **话题** | <简短描述> |
| **受影响组件** | <模块/scope> |
| **最高严重级别** | SEV-N |
| **事故次数** | N |
| **时间跨度** | YYYY-MM-DD 至 YYYY-MM-DD |

## 案例索引

| # | 事故 | 严重级别 | 日期 |
|---|------|----------|------|
| [#N](#事故-n事故简短描述) | <最新事故> | SEV-N | YYYY-MM-DD |

## 话题概述

<该类问题的重复模式、架构边界、影响模块和当前预防策略。追加事故时必须更新。>

---

## 事故 #N：<事故简短描述>

> **Git 范围**：`<from>` → `<to>` | **严重级别**：SEV-N | **日期**：YYYY-MM-DD

### 概要

一至三句话说明故障、影响范围和修复方向。

### 根因分析

#### 类别

<根因分类>

#### 分析

基于实际 diff 的技术解释。必要时引用短代码片段、文件路径和行号。

#### 致因提交

| 提交 | 作者 | 日期 | 描述 |
|------|------|------|------|
| `<sha>` | <作者> | <日期> | <描述> |

如无法确定性定位，明确说明并列出候选。

### 复现

#### 前置条件

- <系统状态、环境变量、运行模式或 app>

#### 步骤

1. <步骤>
2. <步骤>

#### 预期行为

<应发生的行为>

#### 实际行为

<错误输出、trace 缺口、窗口行为、崩溃、挂起或状态漂移>

### 修复

#### 修复提交

| 提交 | 作者 | 日期 | 描述 |
|------|------|------|------|
| `<short>` | <作者> | <日期> | <描述> |

#### 变更内容

<修复做了什么，为什么能修复根因，有哪些局限或 trade-off。>

#### 验证

<新增测试、`bin/verify-smoke`、`dev-ipc` 自动真实窗口验证、trace inspector gate、Maven compile 等。只写真实运行过或 commit 中已有的验证。>

### 影响

- **受影响用户**：<开发者、example app、runtime path、CI/release 等>
- **持续时间**：<可推断则填写，否则写未知>
- **数据影响**：<通常为无；如涉及文件/进程/trace 明确说明>

### 时间线

| 时间 | 事件 |
|------|------|
| YYYY-MM-DD | 候选提交 `<sha>` 引入问题 |
| YYYY-MM-DD | 修复提交 `<sha>` |
| YYYY-MM-DD | 验证完成 |

### 经验教训

#### 做得好的方面

- <已有测试、trace、模块边界或 review 帮助发现/修复的点>

#### 可以改进的方面

- <缺失测试、文档边界、验收盲区或架构债务>

#### 行动项

| 行动 | 优先级 | 状态 |
|------|--------|------|
| <具体行动> | P0/P1/P2 | 待处理 |

### 预防

- **立即执行**：<现在就能加的测试、gate、校验或文档修正>
- **短期**：<本迭代应完成的验收/重构>
- **长期**：<纳入 roadmap 的架构或流程改进>

### 参考

- 修复提交：`<sha>`
- 相关文件：`<path>`
- 相关文档：`<path>`
```

规则：

- 事故组块按时间倒序排列，最新事故在最前。
- 追加已有 topic 时，更新顶部元数据、案例索引、话题概述和 README 统计。
- 同一根因、相近时间（默认 48 小时内）的多个 fix 合并为一个事故组块，并在“修复提交”表中列全。
- 跳过、合并和 reference-only 的 fix 要能在最终回复中解释。

## 验证

写入 postmortem 后至少运行：

```bash
git diff --check -- postmortem
```

如果本次改动还修改了 skill 本身，运行：

```bash
uv run --with PyYAML python /Users/bytedance/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/postmortem
git diff --check -- .agents/skills/postmortem
```

事故报告中的验证必须匹配事故影响面：

- UIDL/adapter/server-side writer：优先 `bash bin/verify-smoke` 或相关 smoke。
- Flow IPC runtime：运行对应 `dev-ipc` 自动真实窗口验证，并按 `docs/acceptance-tools.md` / `DesktopTraceInspector` 使用当前 gate。
- Swift host/window/sidecar：运行 `dev-swift` 或相关 host smoke，结束前停止长时间运行的 dev session。
- 文档-only 事故：至少 `git diff --check`，并确认没有把 compatibility mode 或未完成能力写错。

如果没有运行某项验证，明确写“未运行”及原因。不要把计划中的验证写成已经通过。

## 边缘情况

| 场景 | 行为 |
|------|------|
| 范围内没有 fix 提交 | 回复“在 `<range>` 中未找到 fix 提交，无需生成 postmortem。”不要创建空文件。 |
| tag/ref 不存在 | 报错并列出近期 tag；不要猜测范围。 |
| 工作区不干净 | 不回滚用户改动；只编辑 `postmortem/`，最终说明已有 dirty files。 |
| fix 无 scope | 从修改文件推断组件，例如 `vaadin-desktop-host-swift/` → `host-swift`。 |
| fix 数量很多 | 先分组，再强制代表性筛选；优先 SEV-0 到 SEV-2。 |
| 同一事故跨多个 topic | 在主 topic 写完整事故，其他 topic 只加交叉引用。 |
| 只能推断复现 | 如实写“复现方式需要进一步调查”，不要编造。 |
| 用户明确要求全量记录 | 覆盖代表性筛选，但仍按 topic 分组并合并同根因 follow-up。 |

## 语言要求

- 使用中文撰写 postmortem。
- 使用专业、技术性语言；避免口语化表达。
- 保持免责文化，聚焦系统、流程和边界条件。
- 对不确定性诚实，不虚构 issue、PR、监控或 CI 信息。
- 包含可追溯证据：commit hash、文件路径、关键 diff、验证命令或 trace gate。
- 明确区分“已验证事实”“从 diff 推断”“需要进一步调查”。
