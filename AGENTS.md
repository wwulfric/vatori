# Agent 指令

适用范围：本文件适用于整个仓库。

## 项目定位

本仓库主线是 `vatori`：一个基于 Vaadin 的 Desktop Framework，用于创建桌面级 Vaadin Flow 应用，形态类似 Tauri + Vaadin Flow。

目标不是做一个简单浏览器壳。长期方向是 Flow over IPC：

- 保留 Vaadin Flow 的 Java UI 编程模型。
- 将 Servlet、HTTP、WebSocket 假设隔离在 adapter 后面。
- 桌面 host 拥有系统 WebView、原生能力、生命周期、打包和更新路径。
- IPC 最终成为 bootstrap、event、patch、command 和 capability result 的传输层。

## 当前结构

- `vaadin-desktop-core`：公开的 Java desktop API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace 基础结构。
- `vaadin-desktop-host`：Java host abstraction、显式 compatibility host 和 native bridge support。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 主线的 Swift/AppKit/WKWebView host。
- `test-support/vaadin-desktop-host-swift-acceptance`：仅供仓库内显式 native-host acceptance profile 使用的 Swift/WKWebView 自动化源码和资源；默认 host 构建不得编译或打包。
- `vaadin-desktop-flow`：真实 Vaadin Flow/Spring Boot launcher、标准 production bundle runtime 和 opaque IPC transport adapter。
- `vaadin-desktop-devtools`：标准 production bundle 的 Flow IPC trace inspector。
- `vaadin-desktop-cli`：CLI scaffold。
- `examples/*`：真实 Vaadin Flow 示例应用。
- `test-apps/vaadin-desktop-smoke-app`：独立的开发者形态 smoke app。
- `test-apps/flow-over-ipc-smoke-app`：受限 `dev-ipc` runtime 的 no-HTTP Flow IPC smoke app。
- `test-apps/flow-over-ipc-framework-app`：更接近普通开发者项目形态的 no-HTTP Flow IPC validation app。
- `vaadin-desktop-flow/src/main/java/dev/vatori/desktop/flow/transport`：围绕 `DesktopFlowOpaqueHttpSession`、`DesktopFlowVaadinService`、`DesktopVaadinRequest` 和 `DesktopVaadinResponse` 的 opaque request adapter。

## Runtime 边界

默认 `dev` 行为是在 `127.0.0.1` 随机端口启动真实 Vaadin Flow/Spring Boot 应用，再打开系统 WebView host 加载本地 Vaadin URL。这是真实 desktop WebView host，但 UI transport 仍是 local HTTP/Servlet/WebSocket compatibility mode；不要把它描述为最终 Flow over IPC。

`dev-swift` 运行 `vaadin-desktop-host-swift`：Swift/AppKit host 把 Java app 作为 sidecar 启动，在 sidecar 打印 `Application URL: ...` 后打开 WKWebView 窗口，以 `HOST_PROTOCOL ...` 形式记录 Host Protocol v0 trace，读取 `VDP1 ` Java command envelope，并在窗口关闭时杀掉 Java 进程。它仍通过 localhost HTTP compatibility mode 加载 Flow UI。

`dev-ipc` 是 no-localhost IPC 切片：构建并加载应用自己的标准 Vaadin production bundle，通过 document-start opaque request bridge 和 `VFIPC1` stdin/stdout envelope 把 HTTP-shaped 请求交给 Java 侧 Vaadin request-handler chain。Host 和 bridge 不解析 bootstrap 或 UIDL，也不存在自定义 Flow client shell fallback。

`dev-ipc` 的验证面会持续扩展。`AGENTS.md` 只记录稳定边界和工作约束，不记录具体实现批次、临时状态或 trace gate 名称。当前状态和验收要求以 `docs/flow-over-ipc-runtime.md`、`docs/acceptance-tools.md`、`docs/roadmap.md` 和 `bin/flow-ipc-trace-inspect` 为准。

不要把 bounded validation surface 描述成 full product parity。只要相关能力仍依赖 compatibility mode、fixture hook、fallback path、blocker classification 或 focused validation gate，就必须明确其边界。

## 构建和运行

本机 Maven 通过 SDKMAN 可用。直接调用 Maven 前先 source SDKMAN：

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
```

使用项目本地 Maven repository，避免写入用户全局 Maven cache：

```bash
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

需要运行 Python 工具或临时 Python 脚本时，统一使用 `uv` 管理运行时和依赖，例如 `uv run --with PyYAML python ...`；不要直接使用裸 `python3` 或 `pip install` 来处理项目任务依赖。

常用入口：

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-ipc examples/counter-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

真实窗口、自动化 hook 和 trace gate 验收以 `docs/acceptance-tools.md`、`docs/flow-over-ipc-runtime.md` 和 `bin/flow-ipc-trace-inspect --help` 为准；不要在 `AGENTS.md` 固化单次运行组合。

验证 production opaque adapter 和标准 runtime：

```bash
bash bin/verify-smoke
```

具体子检查由 `bin/verify-smoke` 和 `docs/acceptance-tools.md` 维护，不在本文件
复制其内部命令清单。

Host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

`system-webview` 默认 fail-fast；需要 compatibility host 时显式设置
`VAADIN_DESKTOP_HOST=compatibility`、`headless` 或 `browser`。只有在本地开发需要
自动兜底时，才设置 `VAADIN_DESKTOP_HOST_FALLBACK=compatibility`。

完整 `.ndjson` trace 是生成产物，不按版本逐个提交。验收契约由 `bin/flow-ipc-trace-inspect` 暴露；需要长期保存的 trace 应进入 CI artifact、release asset 或外部归档。

启动 system WebView host 会打开 macOS UI 进程。结束一个 turn 前，应停止长时间运行的 dev session。

## 本地参考源码树

用户本机有上游源码 checkout 可供研究。除非用户明确要求编辑，否则把它们视为只读参考。

Tauri：

```text
/Users/bytedance/RustroverProjects/study/tauri
```

常用区域：

- `crates/tauri`
- `crates/tauri-runtime`
- `crates/tauri-runtime-wry`
- `crates/tauri-cli`
- `crates/tauri-bundler`
- `crates/tauri-utils`

Vaadin：

```text
/Users/bytedance/IdeaProjects/study/vaadin
```

常用区域：

- `flow/flow-server`
- `flow/flow-client`
- `flow/flow-push`
- `flow/vaadin-spring`
- `platform/vaadin-spring-boot-starter`
- `platform/vaadin-maven-plugin`
- `platform/vaadin-dev-bundle`

做架构决策时，优先检查这些本地源码树，再考虑 web search。

## 文档地图

- `README.md`：项目入口地图和 quick start。
- `docs/framework-architecture.md`：当前架构与目标架构。
- `docs/getting-started.md`：开发者 workflow。
- `docs/flow-integration.md`：今天如何集成真实 Vaadin Flow。
- `docs/ipc-protocol.md`：protocol envelope 和 message taxonomy。
- `docs/ipc-protocol-v1.md`：Flow IPC v1 envelope 和 message taxonomy。
- `docs/flow-over-ipc-runtime.md`：当前 `dev-ipc` runtime 切片和限制。
- `docs/flow-transport-adapter.md`：当前 adapter 实现和下一个 UIDL 边界。
- `docs/acceptance-tools.md`：trace gate、smoke harness、真实窗口验收 hook 和 runtime diagnostics 的边界说明。
- `docs/native-host.md`：当前 WKWebView host 和 host roadmap。
- `docs/roadmap.md`：工程路线。
- `docs/flow-over-ipc-design.md`：历史中文设计包；当前实现状态以 runtime、acceptance、roadmap 和架构文档为准。

## 项目技能

- `.agents/skills/flow-component-parity`：当用户要求推进 Vaadin Flow 组件产品级 parity、Flow over IPC 组件覆盖、或从长期目标中选择并完成下一个可验证切片时使用。
- `.agents/skills/vaadin-desktop-native-feel`：当工作触及 Swift/AppKit host 行为、WKWebView/WebView2 quirk、Vaadin Flow desktop UI 约定、desktop capability API、compatibility-mode 边界或 native-readiness review 时使用。
- `.agents/skills/postmortem`：当需要从指定 git 范围分析 `fix:` 提交、筛选代表性事故，并生成或追加 vatori 专用中文复盘报告到 `postmortem/` 时使用。

## 工程规则

- 保持 compatibility mode 被明确命名和记录。
- 不要移除或隐藏当前 Flow 仍然依赖 Servlet/HTTP/WebSocket 的事实。
- 不要把框架收缩成一次性的示例应用。
- 保持 example app 是开发者形态：它们应该像外部用户一样依赖 framework modules。
- 除非明确要求，不要编辑本地 Tauri 或 Vaadin upstream source checkout。
- 优先使用小而可 review 的模块和清晰 adapter 边界。
- 搜索源码时使用 `rg` / `rg --files`。
- 文件编辑使用 `apply_patch`。
- 文档变更至少运行 `git diff --check`。
- 如果用户要求提交，按适合粒度提交 commit；提交信息使用 Conventional Commit 前缀，例如 `feat:`、`fix:`、`docs:`、`test:`、`chore:`、`refactor:`。
- 除非明确要求，不要删除生成文件或用户文件。

## 顶层文档规则

- `AGENTS.md` 是长期稳定的 agent 工作约束，不是进度日志、验收记录或临时决策缓存。
- `README.md` / `README.zh-CN.md` 是项目入口地图，只描述稳定定位、运行方式、验证入口和高层状态。
- 不要在这些顶层文件里写入具体实现批次、trace gate 名称、单次运行结果、长篇历史过程、误冲突内容或其他项目的说明。
- 具体状态、验收细节和历史证据应写入 `docs/flow-over-ipc-runtime.md`、`docs/acceptance-tools.md`、`docs/roadmap.md`、专门的设计文档或 inspector 代码。
- 除非项目定位、稳定命令、长期约束或公开入口发生变化，不要顺手修改这些顶层文件。
- 修改这些顶层文件后，必须运行 `git diff --check -- AGENTS.md README.md README.zh-CN.md` 和 `bash bin/repository-hygiene-check`。

## 工作选择规则

- 推进 Flow 组件 parity 时，每轮选择一个最小、可验证、可收口的切片；不要同时展开多个无关组件面。
- 不要为了记录过程新建 `docs/goal-*.md` 或归档目录。当前目标和状态应收敛到现有 roadmap、runtime、acceptance 文档和代码验收面。
- 如果需要保存单次运行证据，优先使用最终回答、git history、CI artifact、release asset 或外部归档，而不是仓库内过程日志。
- 当无法在本轮闭合产品切片时，优先补齐能解除阻塞的 adapter、trace inspector、smoke harness 或文档契约。
