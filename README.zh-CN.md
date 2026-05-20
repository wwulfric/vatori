# Vaadin Desktop Framework v0

本仓库探索一个受 Tauri/Raycast 风格进程边界启发的 Vaadin Flow 桌面 runtime。当前交付物是一个真实框架骨架，不只是早期的 mock IPC spike。

Framework v0 默认运行在 **compatibility mode**：真实 Vaadin Flow view 由嵌入式 Spring Boot 在 `127.0.0.1` 上提供服务，同时 desktop runtime API、host 边界、IPC envelope 和 trace log 已经分离出来，以便后续演进为 Flow over IPC。

仓库现在也包含第一个 `dev-ipc` runtime 切片。它运行一个受限的 Flow-over-IPC smoke app，核心 UI 路径不使用 localhost HTTP server；它把 WebView click、原生 input `change`、checkbox `change`、一个真实 Vaadin Web Component `change` 和 recovery resync 作为 Flow-client-shaped UIDL RPC payload 发送，发出真实 server-side Vaadin UIDL，并通过 `UidlRequestHandler` / `ServerRpcHandler` dispatch 这些消息。Bootstrap、两个 ordered server-initiated push、event patch 和 recovery resync 由可见 Flow client engine 消费；一个受控 duplicate/late push probe 会被忽略并触发显式 resync diagnostics。Bootstrap 还会声明真实 Vaadin `flow-client` classpath resources；WebView 通过 IPC 请求并验证完整 advertised resource graph，导入并初始化 `FlowBootstrap.js` 和增强版 `FlowClient.js`，记录 executed vs verified-only resource classification，并对 `Flow.js` 执行最小 no-HTTP lazy lookup probe。v1.2 还会通过 IPC 请求并执行 `com.vaadin.flow.component.textfield.BigDecimalField` 的真实 classpath frontend module `META-INF/resources/frontend/vaadin-big-decimal-field.js`；缺失的 npm transitive imports 会被明确分类并通过 v1.2-only shim 补齐，因此这只是第一个 Web Component 边界证据，不是完整 TextField/npm/dev-mode parity。Desktop capability gateway 也有一个受限 v1.1 file-transfer smoke 边界，用于 controlled-path download 写入和 upload/select-file metadata 读取。它仍然不是完整 Flow transport parity，因为 broad component/resource coverage、durable push queue/reconnect、完整 upload/download streaming 和 production bundle/dev-mode semantics 仍是未来工作。

## 快速开始

要求：

- JDK 21+。
- Maven 3.9+。
- 当前 system WebView host 需要带 Swift toolchain 的 macOS。
- Swift/AppKit native host 需要带 Swift toolchain 的 macOS。
- 如果 Maven 通过 SDKMAN 安装，`bin/vaadin-desktop` 会自动 source SDKMAN。

运行 counter 示例：

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

运行独立 smoke app：

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

运行下一阶段 Swift native host 路径：

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

`dev-swift` 会构建 `VaadinDesktopHost.app` 并通过 macOS LaunchServices 启动它，把 Java Vaadin app 作为 sidecar 启动，等待 compatibility URL，打开带 `WKWebView` 的 AppKit `NSWindow`，并观察 Java 进程生命周期。Host 日志会从 `.build/vaadin-desktop-host-swift/host.out.log` 和 `.build/vaadin-desktop-host-swift/host.err.log` tail 出来；WebView 渲染后，host 还会写入 `.build/vaadin-desktop-host-swift/window-snapshot.png`。Desktop API call 现在可以从 Java 向 host 发出 Host Protocol v0 command envelope。Flow UI transport 仍然使用 localhost HTTP，所以这是长期 host/runtime prototype，还不是最终 Flow over IPC。

运行第一个 Flow IPC runtime 切片：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`dev-ipc` 会用 `transport=flow-ipc` 启动同一个 Swift/AppKit host，加载本地 WKWebView shell，通过 `VFIPC1` stdin/stdout envelope 将 Flow client message 发送给 Java sidecar，并在不加载 `http://127.0.0.1` 的情况下返回 Java patch。Bootstrap payload 包含来自 `UidlWriter` 的真实 Vaadin server-side UIDL；runtime 还会发出两个 ordered server-initiated push UIDL update 和一个受控 duplicate/late probe。WebView bridge 会把 click、原生 input `change`、checkbox `change`、`BigDecimalField` Web Component `change` 和 ordering-recovery resync 作为 UIDL RPC body 发送，body 包含 CSRF、`syncId`、`clientId`、event data 和 Flow node id，然后 Java 通过 `UidlRequestHandler` / `ServerRpcHandler` 处理。Bootstrap 会声明 Flow client classpath resources 和 v1.2 component frontend resources；WebView 请求并验证完整 advertised resource graph，导入 `FlowBootstrap.js` 和 `FlowClient.js`，以明确的 v1.2 shim 导入真实 `vaadin-big-decimal-field.js` module，对 `Flow.js` 发起动态 `flow-client-lazy-lookup` request，记录 executed / verified-only / intentionally-not-executed / execution-failed classification，并通过可见 Flow client engine 消费 bootstrap、push、event 和 resync DOM update。Desktop capability result 现在支持 `allow-all-dev`、`manifest-enforced` 和 `trace-only-dry-run` permission mode；smoke app 使用 `manifest-enforced`，触发 AppKit `NSOpenPanel` cancellation，发出 manifest deny-path，把受限 download body 写入 controlled path，并从 controlled test file 读取 upload/select-file metadata。Verification hook 可以发出一个 WebView-originated `error` envelope；仓库本地 UIDL applier 只保留为诊断 fallback。

无人值守真实窗口验证时，在 `dev-ipc` 命令前加上 `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1`。File dialog hook 会触发 AppKit `NSOpenPanel` 路径并自动取消；Web Component hook 会在组件 frontend module 导入后触发 `BigDecimalField` DOM `change` event；shutdown hook 会在 DOM/snapshot capture 后关闭窗口，使 host log 包含 shutdown evidence。

检查 Flow IPC trace 示例：

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
```

验证第一个真实 Flow UIDL writer adapter 切片：

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

这些命令会在进程内构造一个最小 desktop-shaped `VaadinSession` / `UI`。第一个命令验证 Vaadin 的真实 `UidlWriter` 会发出 UIDL `changes`、`syncId` 和 bootstrap `Vaadin-Security-Key`；第二个命令把真实 Flow event RPC body 通过 `UidlRequestHandler` / `ServerRpcHandler` 发送，验证 click listener UIDL patch，并确认无效 Flow IPC `csrfToken` / security key、过期 `syncId` 和乱序 `clientId` 会在 dispatch 前被拒绝。第三个命令验证无效 Flow IPC envelope `sessionId` / `uiId` 会在 event handling 前被拒绝。第四个命令验证 runtime 可以读取并响应每一个 advertised Vaadin `flow-client` classpath resource，包括 `FlowBootstrap.js`、`FlowClient.js`、`Flow.js`、`index.js`、`compile.properties` 和生成的 GWT client bundle。

在 macOS 上，原始 `dev` 命令默认仍会打开原生 WKWebView desktop window。该路径现在是 macOS fallback / compatibility host。

Host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

## 模块

- `vaadin-desktop-core`：Java desktop runtime API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace logging primitives。
- `vaadin-desktop-host`：macOS system WebView fallback、compatibility host 和 Java native bridge 实现。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 路径的 Swift/AppKit/WKWebView host。
- `vaadin-desktop-flow`：真实 Vaadin Flow/Spring Boot integration layer，加上第一个已编译 UIDL transport adapter 切片。
- `vaadin-desktop-devtools`：用于 Flow IPC review trace 的 trace inspector。
- `vaadin-desktop-cli`：CLI scaffold；shell 入口是 `bin/vaadin-desktop`。
- `examples/counter-app`：真实 Vaadin Flow counter app。
- `examples/grid-app`：真实 Vaadin Flow Grid app。
- `examples/desktop-capabilities-app`：desktop runtime API 示例。
- `test-apps/vaadin-desktop-smoke-app`：使用 framework 的独立测试项目。
- `test-apps/flow-over-ipc-smoke-app`：面向 `dev-ipc` runtime 切片的独立测试项目。
- `flow-transport-adapter-spike`：代码级 Vaadin Flow transport 边界分析和 adapter 形态。
- `prototypes/phase0-phase1-spike`：更早的非 HTTP mock transport 和 IPC 验证。

## 文档

- [Framework Architecture](docs/framework-architecture.md)
- [Getting Started](docs/getting-started.md)
- [Flow Integration](docs/flow-integration.md)
- [IPC Protocol](docs/ipc-protocol.md)
- [IPC Protocol v1](docs/ipc-protocol-v1.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [Native Host](docs/native-host.md)
- [Roadmap](docs/roadmap.md)
- [Original Design Package](docs/flow-over-ipc-design.md)

## 当前状态

已实现：

- 真实 Vaadin Flow application modules。
- Framework-level Java desktop API：window title、notification、clipboard、file dialog、bounded file transfer 和 menu facade。
- macOS system WebView launcher：Spring Boot 在随机 localhost port 启动 Vaadin；生成的 WKWebView `.app` 打开 desktop window。
- Swift/AppKit host：启动 Java sidecar，打开 WKWebView，记录 Host Protocol v0 trace，并在窗口关闭时清理 sidecar。
- Flow IPC runtime 切片：启动 no-HTTP smoke app，通过 IPC 和 `UidlRequestHandler` / `ServerRpcHandler` dispatch Button click，生成真实 server-side UIDL，把 desktop API call 作为 `VFIPC1 command` 发送，接收 `capabilityResult`，并让可见 Flow client engine 消费 bootstrap、ordered push、event 和 recovery resync UIDL。
- Flow IPC push ordering smoke：Java 发出两个 ordered server push UIDL update 加一个受控 duplicate/late probe；WebView 记录 delivered/ignored order，并触发显式 ordering-recovery resync。
- Flow client resource loading probe：`dev-ipc` 声明 Vaadin `flow-client` classpath resources，WebView 通过 `resourceRequest` 请求完整 advertised resource graph，Java 用 redacted `resourceResponse` trace 响应，diagnostics 验证所有 response 加上真实 Flow bootstrap 和 GWT client module import/init/binding；v1.1 DOM evidence 将资源分类为 executed、verified-only、intentionally not executed 或 execution-failed。同一 channel 现在还包含面向 `Flow.js` 的动态 `flow-client-lazy-lookup` probe。
- 第一个真实 Vaadin Web Component 切片：smoke app 使用 `com.vaadin.flow.component.textfield.BigDecimalField`，WebView 通过 IPC 请求真实 `vaadin-big-decimal-field.js` classpath frontend module，用明确分类的 v1.2 shim 处理缺失 npm imports，触发升级后的 `vaadin-big-decimal-field` DOM `change` event，并验证 Java listener -> `UidlRequestHandler` patch -> 可见 Flow client DOM 状态 `Vaadin BigDecimalField: 42.5`。
- Flow IPC file-transfer smoke：Java 通过 native capability gateway 发送 `fileTransfer.download` 和 `fileTransfer.selectUploadFile` command；Swift 把受限 body 写入 controlled test path，读取 selected-file metadata 和 sampled byte count，在 trace 中 redacts body content，并报告 typed `capabilityResult` metadata。
- UIDL adapter smoke：从进程内 desktop-shaped session/UI 边界驱动 Vaadin 的真实 server-side `UidlWriter`。
- UIDL RPC smoke：通过 `UidlRequestHandler` / `ServerRpcHandler` 驱动真实 Flow event RPC，并验证 listener-produced UIDL patch。
- Java desktop command 的 Host Protocol v0 stdout envelope。
- Flow transport adapter spike，其中包含 UIDL/event/patch 边界的具体 Vaadin 源码引用。
- 面向 headless/browser mode 的 compatibility host fallback。
- IPC trace shape 和 command envelope。
- 依赖 framework modules 的独立 smoke app。

尚未实现：

- 超出受限 smoke app 的 broad Flow component 和 event coverage。
- 超出当前 classification/lazy-lookup/component-shim diagnostics 的完整 Flow client UIDL parity、dev mode replacement 和 production frontend bundle execution。
- 超出受控 duplicate/late ordering recovery smoke 的 durable Flow push/reconnect semantics。
- 完整 Vaadin Upload/Download parity、binary streaming、progress 和 sandbox-safe file grants。
- 生产级 native capability permission model。
- 生产级 packaging、signing、auto-update。
