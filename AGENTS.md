# Agent 指令

适用范围：本文件适用于整个仓库。

## 项目目标

本仓库正在构建 `Vaadin Desktop Framework v0`：一个用于创建桌面级 Vaadin Flow 应用的真实框架骨架，形态类似 Tauri + Vaadin Flow。

目标不是做一个简单的浏览器壳。长期方向是 Flow over IPC：

- 保留 Vaadin Flow 的 Java UI 编程模型。
- 将 Servlet/HTTP/WebSocket 假设隔离在 adapter 后面。
- 桌面 host 拥有系统 WebView、原生能力、生命周期、打包和更新路径。
- IPC 最终成为 bootstrap、event、patch、command 和 capability result 的传输层。

## 当前状态

当前实现是一个真实框架骨架，加上一条 compatibility transport：

- `vaadin-desktop-core`：公开的 Java desktop API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace 基础结构。
- `vaadin-desktop-host`：macOS WKWebView fallback、compatibility host 和 Java native bridge 实现。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 主线的 Swift/AppKit/WKWebView host spike。
- `vaadin-desktop-flow`：真实 Vaadin Flow/Spring Boot launcher，加上 Flow IPC runtime 和 UIDL adapter 切片。
- `vaadin-desktop-devtools`：带 Flow IPC v1 检查的 trace inspector scaffold。
- `vaadin-desktop-cli`：CLI scaffold。
- `examples/*`：真实 Vaadin Flow 示例应用。
- `test-apps/vaadin-desktop-smoke-app`：独立的开发者形态 smoke app。
- `test-apps/flow-over-ipc-smoke-app`：面向受限 `dev-ipc` runtime 切片的独立 no-HTTP Flow IPC smoke app。
- `test-apps/flow-over-ipc-framework-app`：更接近普通开发者项目形态的 no-HTTP Flow IPC validation app，覆盖 Button、BigDecimalField、Checkbox、Select、重复列表更新和 desktop capability；strict trace gate 仍然以 smoke app 为准。
- `prototypes/phase0-phase1-spike`：更早的非 HTTP mock Flow transport / IPC 验证。
- `flow-transport-adapter-spike`：当前代码级 Vaadin Flow UIDL/event/patch 边界分析。
- `vaadin-desktop-flow/src/main/java/dev/vatori/desktop/flow/transport`：围绕 `UidlRequestHandler`、`UidlWriter`、`DesktopVaadinRequest`、`DesktopVaadinResponse` 和 `DesktopFlowRuntimeSession` 的已编译 UIDL adapter 切片。

macOS 上默认 `dev` 行为：

1. 在 `127.0.0.1` 随机端口启动真实 Vaadin Flow/Spring Boot 应用。
2. 启动 `SystemWebViewDesktopHost`。
3. 使用 `/usr/bin/swiftc` 编译临时 Swift/WKWebView `.app` bundle。
4. 打开加载本地 Vaadin URL 的原生桌面窗口。

重要边界：这是真实 desktop WebView host，但 UI transport 仍然是 local HTTP/Servlet/WebSocket compatibility mode。不要把它描述为最终 Flow over IPC。

长期 host 路径：

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

这会运行 `vaadin-desktop-host-swift`：它把 Java app 作为 sidecar 启动，在 sidecar 打印 `Application URL: ...` 后打开原生 AppKit/WKWebView 窗口，以 `HOST_PROTOCOL ...` 形式记录 Host Protocol v0 trace，读取以 `VDP1 ` 为前缀的 Java command envelope，并在窗口关闭时杀掉 Java 进程。它仍然通过 localhost HTTP compatibility mode 加载 Flow UI。

`dev-swift` 通过 macOS LaunchServices 启动生成的 `.build/vaadin-desktop-host-swift/VaadinDesktopHost.app`。日志会从 `.build/vaadin-desktop-host-swift/host.out.log` 和 `.build/vaadin-desktop-host-swift/host.err.log` tail 出来。真实窗口验证时，检查 `HOST_PROTOCOL webview.dom` 和 `.build/vaadin-desktop-host-swift/window-snapshot.png`；在受管 macOS 桌面上，全局截图可能会被 `loginwindow` / MDM overlay 遮挡。

No-HTTP IPC 切片：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

这会用 `--transport flow-ipc` 运行 Swift host，加载本地 WKWebView HTML shell，并通过 `VFIPC1` stdin/stdout envelope 与 Java 通信。它证明了 smoke app 在没有 localhost HTTP 的情况下完成 WebView event -> Java Button listener -> desktop command/capabilityResult -> patch -> WebView。WebView bridge 会把 click/change event、`BigDecimalField` Web Component change event、`Checkbox` Web Component change event、真实 `TextField` Web Component change event 和 ordering-recovery resync 作为 Flow-client-shaped UIDL RPC payload 发送；Java 在 `payload.uidl` 下发出真实 UIDL，其中 bootstrap 和 ordered server-initiated push 使用 `UidlWriter`，event/resync 使用 `UidlRequestHandler` / `ServerRpcHandler`。Bootstrap、ordered push、event patch 和 recovery resync 都由可见 Flow client engine 消费。一个受控 duplicate/late push probe 会被忽略，并记录 delivered/ignored order diagnostics，然后触发显式 resync。Bootstrap 还会声明真实 Vaadin `flow-client` classpath resources 和 v1.4 frontend artifact manifest；WebView 通过 IPC 请求、验证并分类 advertised resource graph，在 `resourceRequest` 中回显 artifact family、module specifier、dependencyOf、executionPolicy、resolver 和 status metadata，同时导入并初始化 `FlowBootstrap.js` 和增强版 `FlowClient.js`。component frontend manifest 会声明 `BigDecimalField`、`TextField`、`Checkbox` 和 `Select` 四个真实 Flow component：BigDecimalField 请求并执行真实 `META-INF/resources/frontend/vaadin-big-decimal-field.js` classpath 组件前端模块，其 `lit` import 解析到 focused extracted Lit CSS helper，`@vaadin/component-base/src/define.js` 解析到 focused extracted define helper，`@vaadin/text-field` import 已改为真实 `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js` entry；Checkbox/Select 也通过 IPC 请求聚焦 dev-bundle 入口，从 Vaadin production `indexhtml` chunk 导入真实 class 并注册 `vaadin-checkbox` / `vaadin-select`。v1.5 在 WebView/host 边界加入 no-localhost frontend staging resolver：真实 artifact body 被 stage 为 Blob module URL，每个 staged artifact 都会记录 `resourceStage`，BigDecimalField 和 Checkbox/Select 通过 staged path 执行；TextField production class entry 会通过 staging 尝试，真实 Vaadin dev-bundle `generated-flow-imports-*.js` chunk 会进入 generated/lazy staged execution attempt，并记录当前 `Flow.loadOnDemand` API 边界和浏览器错误。v1.6 把 TextField/npm graph blocker 收敛到更精确的 staged graph 证据。v1.7 通过 stable resource-path Blob module identity 复用 production `indexhtml` staged dependency，越过 v1.6 duplicate custom element definition 层，记录 `textfieldExecutionVersion=flow-ipc-textfield-v1_7`、`textfieldSideEffectVersion=flow-ipc-textfield-side-effect-v1_7`、module identity、side-effect strategy 和 `remainingBlockerClassification=none`，并验证真实 Flow `TextField` Java listener -> UIDL patch -> 可见 DOM `Vaadin TextField: Flow IPC typed`。剩余 TextField shim 只作为旧路径回归保护，不能计入 v1.7 成功；production/dev-mode/Vite execution 和完整 generated/lazy execution 仍是 strict blocker。这不是完整 production bundle/dev-mode/theme parity。它还会对 `Flow.js` 执行动态 no-HTTP `flow-client-lazy-lookup` probe，并记录 static/theme/lazy lookup diagnostics。仓库内的本地 UIDL applier 只保留为 non-engine 或 failed-delivery 路径的诊断 fallback。

可重复的真实窗口 smoke 验证可以使用：

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

## 构建和运行

本机 Maven 通过 SDKMAN 可用。直接调用 Maven 前先 source SDKMAN：

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
```

使用项目本地 Maven repository，避免写入用户全局 Maven cache：

```bash
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

运行 smoke desktop app：

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

运行 Swift native host spike：

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

运行 Flow IPC smoke app：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

运行 Flow IPC framework validation app：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

可重复的 framework app 真实窗口验证可以使用：

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

检查 Flow IPC trace 示例：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
```

验证 server-side UIDL writer adapter：

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

Host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

启动 system WebView host 会打开 macOS UI 进程，可能需要 sandbox escalation。结束一个 turn 前，应停止长时间运行的 dev session。

## 本地参考源码树

用户本机有上游源码 checkout 可供研究。除非用户明确要求编辑，否则把它们视为只读参考。

Tauri，仅作为历史参考：

```text
/Users/bytedance/RustroverProjects/study/tauri
```

如果要对比 Tauri 的进程模型，常用区域：

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

常用 Vaadin 区域：

- `flow/flow-server`
- `flow/flow-client`
- `flow/flow-push`
- `flow/vaadin-spring`
- `platform/vaadin-spring-boot-starter`
- `platform/vaadin-maven-plugin`
- `platform/vaadin-dev-bundle`

做架构决策时，优先检查这些本地源码树，再考虑 web search。

## 文档地图

- `README.md`：当前 quick start 和状态。
- `docs/framework-architecture.md`：当前架构与目标架构。
- `docs/getting-started.md`：开发者 workflow。
- `docs/flow-integration.md`：今天如何集成真实 Vaadin Flow。
- `docs/ipc-protocol.md`：protocol envelope 和 message taxonomy。
- `docs/ipc-protocol-v1.md`：Flow IPC v1 envelope 和 message taxonomy。
- `docs/flow-over-ipc-runtime.md`：当前 `dev-ipc` runtime 切片和限制。
- `docs/flow-transport-adapter.md`：当前 adapter 实现和下一个 UIDL 边界。
- `docs/native-host.md`：当前 WKWebView host 和 host roadmap。
- `docs/roadmap.md`：下一阶段工程路线。
- `docs/flow-over-ipc-design.md`：原始中文设计包。
- `docs/flow-over-ipc-implementation.md`：Phase 0/1 prototype notes。

## 项目技能

- `.agents/skills/vaadin-desktop-native-feel`：用于让 Vaadin Desktop app 具备原生桌面体验的项目级 skill。当工作触及 Swift/AppKit host 行为、WKWebView/WebView2 quirk、Vaadin Flow desktop UI 约定、desktop capability API、compatibility-mode 边界或 native-readiness review 时使用它。

## 工程规则

- 保持 compatibility mode 被明确命名和记录。
- 不要移除或隐藏当前 Flow 仍然依赖 Servlet/HTTP/WebSocket 的事实。
- 不要把框架收缩成一次性的示例应用。
- 保持 example app 是开发者形态：它们应该像外部用户一样依赖 framework modules。
- 除非明确要求，不要编辑本地 Tauri 或 Vaadin upstream source checkout。
- 优先使用小而可 review 的模块和清晰 adapter 边界。
- 搜索源码时使用 `rg` / `rg --files`。
- 文件编辑使用 `apply_patch`。
- 每轮工作按适合粒度提交 commit；提交信息使用 Conventional Commit 前缀，例如
  `feat:`、`fix:`、`docs:`、`test:`、`chore:`、`refactor:`。
- 除非明确要求，不要删除生成文件或用户文件。

## 近期工作

最有价值的下一步：

1. 在 `vaadin-desktop-host-swift` 跨本地机器稳定后，把它作为默认 host。
2. 将 Host Protocol v0 trace 推进为 bidirectional command/result transport。
3. 基于 v1.7 TextField stable module identity 结果，继续拆解 generated/lazy `Flow.loadOnDemand`、production bundle execution 或 dev-mode/Vite execution strict blocker。
4. 为 Swift host window launch、sidecar cleanup 和 desktop capability call 增加 smoke tests。
5. 将受控 duplicate/late push probe 推进为 durable queue、ack、reconnect 和 resync 语义。
