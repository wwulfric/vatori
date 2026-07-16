# Agent 指令

适用范围：整个仓库。

## 项目定位

vatori 是基于 Vaadin 的 Desktop Framework，不是某一个桌面应用。它保留 Vaadin
Flow 的 Java UI 编程模型，并让 native host 拥有系统 WebView、原生能力、生命周期、
平台打包和更新边界。

主架构是标准 Vaadin production bundle + opaque Flow IPC。不要实现自定义 Flow
client、UI renderer 或按组件解释 UIDL 的 host runtime。

## 仓库心智模型

模块职责、依赖方向、发布边界和代码放置规则统一以
`docs/repository-structure.md` 为准。开始跨模块修改或新增目录前先阅读该文档；不要
在 `AGENTS.md` 中复制目录清单或维护第二份结构说明。

稳定运行主链：

```text
bin/vaadin-desktop
  -> native-hosts/macos
  -> AppKit + WKWebView
  -> Java sidecar
  -> vaadin-desktop-flow
```

平台和构建产物选择 native host。Java 应用不能选择 host，也不能包含浏览器、headless
或 compatibility fallback。当前只有 macOS 实现；不要为了表达未来方向创建空的
Windows/Linux 模块。

## Runtime 边界

`dev` 是默认主线：CLI 构建应用的标准 Vaadin production bundle，macOS host 从本地
resource scheme 加载它，并通过 document-start opaque request/push bridge 与 Java
sidecar 交换 VFIPC1 envelope。Host 和 bridge 不解析 bootstrap 或 UIDL。

`dev-compat` 使用同一个 macOS host，但 WKWebView 加载 Java sidecar 发布的随机
`127.0.0.1` URL。Servlet、HTTP 和 WebSocket 仍属于 compatibility UI transport；
native command/result、window lifecycle 和 shutdown 继续使用统一 VFIPC1 channel。

`DesktopLauncher.launch(...)` 是阻塞式应用入口。没有合法 sidecar transport 时，
直接运行应用 JAR 必须失败。Java `ready` 提供 application metadata 和 main-window
descriptor，host 验证后才能创建窗口。

不要把当前验证面描述成 full product parity。只要能力仍依赖 focused validation、
fixture、compatibility transport 或未完成的产品边界，就明确说明限制。

## 构建和运行

本机 Maven 通过 SDKMAN 可用。直接调用 Maven前先加载：

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
```

使用仓库本地 Maven repository：

```bash
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

需要 Python 时统一使用 `uv` 管理运行时和依赖，不要直接使用裸 `python3` 或
`pip install` 处理项目依赖。

稳定开发入口：

```bash
bash bin/vaadin-desktop dev examples/counter-app
bash bin/vaadin-desktop dev-compat examples/counter-app
bash bin/vaadin-desktop package examples/counter-app
bash bin/verify-smoke
```

具体 acceptance 组合以 `docs/acceptance-tools.md` 和
`bin/flow-ipc-trace-inspect --help` 为准，不在本文件复制单次运行参数。

完整 trace、日志、截图和 native app 都是生成产物。长期证据应进入 CI artifact、
release asset 或外部归档。启动真实窗口后，结束 turn 前停止长时间运行的 dev session。

## 本地参考源码

用户本机有只读上游 checkout。除非用户明确要求，不要编辑：

- Tauri：`/Users/bytedance/RustroverProjects/study/tauri`
- Vaadin：`/Users/bytedance/IdeaProjects/study/vaadin`

常用区域：

- Tauri：`crates/tauri`、`crates/tauri-runtime`、`crates/tauri-runtime-wry`、
  `crates/tauri-cli`、`crates/tauri-bundler`、`crates/tauri-utils`
- Vaadin：`flow/flow-server`、`flow/flow-client`、`flow/flow-push`、
  `flow/vaadin-spring`、`platform/vaadin-spring-boot-starter`、
  `platform/vaadin-maven-plugin`

做架构决策时优先检查这些本地源码，再考虑 web search。

## 文档地图

- `README.md` / `README.zh-CN.md`：公开入口和稳定命令。
- `docs/repository-structure.md`：仓库心智模型和放置规则。
- `docs/framework-architecture.md`：运行时所有权和 transport 边界。
- `docs/getting-started.md`：开发者 workflow。
- `docs/flow-integration.md`：Vaadin Flow 集成方式。
- `docs/ipc-protocol.md`：唯一 sidecar wire protocol。
- `docs/flow-over-ipc-runtime.md`：当前 Flow IPC 能力和限制。
- `docs/flow-transport-adapter.md`：request/push adapter 边界。
- `docs/acceptance-tools.md`：smoke、真实窗口和 trace inspector。
- `docs/native-host.md`：macOS host contract。
- `docs/roadmap.md`：工程路线。

## 项目技能

- `.agents/skills/flow-component-parity`：选择并闭合一个可验证的 Flow parity slice。
- `.agents/skills/vaadin-desktop-native-feel`：native host、desktop UI 和 capability review。
- `.agents/skills/postmortem`：从 git fix 范围生成事故复盘。

## 工程规则

- 保持 compatibility HTTP 被明确命名，不隐藏 Servlet/HTTP/WebSocket 依赖。
- framework production sources 和 resources 只能包含通用框架能力；fixture、自动化、
  故障注入和 gate 必须放在 `src/test`、`test-apps` 或 `test-support`。
- examples 和 test apps 必须像外部用户一样依赖 framework modules，并通过
  `@SpringBootApplication + DesktopLauncher` 启动。
- 根 Maven reactor 聚合 framework、examples 和 test apps；`test-support` 由验证命令
  显式构建，不能成为 framework artifact 的依赖。
- Standard bundle 的构建和打包属于 CLI/native-host build；Flow module 只处理 Java
  sidecar runtime 和 transport adapter。
- 所有浏览器请求统一进入 `VaadinService.handleRequest()`。平台 host 不得按 bootstrap、
  UIDL、Upload 或 Download 类型分支。
- 搜索源码使用 `rg` / `rg --files`；文件编辑使用 `apply_patch`。
- 文档变更至少运行 `git diff --check`。
- 用户要求提交时使用合适粒度和 Conventional Commit 前缀。
- 除非明确要求，不删除用户文件；不要覆盖未提交的并行工作。

## 顶层文档规则

`AGENTS.md` 是长期 agent 约束，不是进度日志、验收记录或临时决策缓存。
README 是项目入口地图，只记录稳定定位、命令、验证入口和高层边界。

不要在顶层文件写具体实现批次、trace gate 名称、单次运行结果、goal 版本、误冲突
内容或长篇历史过程。除非稳定入口或长期约束发生变化，不要顺手修改这些文件。

修改顶层文件后必须运行：

```bash
git diff --check -- AGENTS.md README.md README.zh-CN.md
bash bin/repository-hygiene-check
```

## 工作选择规则

- 推进 Flow parity 时，每轮只选择一个最小、可验证、可收口的 slice。
- 不为过程记录创建 `docs/goal-*.md`、归档目录或版本化源码名称。
- 当前状态收敛到现有 runtime、acceptance、roadmap 文档和可执行验收面。
- 单次运行证据优先保留在最终回答、git history、CI artifact 或 release asset。
- 本轮无法闭合产品 slice 时，优先补齐能解除阻塞的 adapter、inspector、smoke 或
  文档契约。
