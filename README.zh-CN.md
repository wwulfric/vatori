# vatori

vatori 是一个基于 Vaadin 的 Desktop Framework，用于构建桌面级 Vaadin Flow 应用的实验性框架骨架。它的方向类似 Tauri 加 Vaadin Flow：保留 Java Flow 编程模型，运行在原生桌面 host 中，并逐步把浏览器/服务器传输假设收束到明确的 host 和 IPC 边界后面。

这不是一个已经完成的应用壳，也还不是最终版 Flow over IPC runtime。当前仓库有两条有用的运行路径：

- **Compatibility mode**：真实 Vaadin Flow / Spring Boot app 运行在 `127.0.0.1`，原生 macOS WebView host 打开桌面窗口。
- **Flow IPC 切片**：面向受限 Flow 验证面的实验性 no-localhost 路径。已覆盖的验证面里，bootstrap、event、UIDL patch、frontend resource 和部分 desktop capability 通过 IPC 传输。

README 只保留入口地图；详细的 Flow IPC 证据和设计说明放在 `docs/` 里。

## 快速开始

要求：

- JDK 21+
- Maven 3.9+
- 当前原生 WebView host 需要带 Swift toolchain 的 macOS
- 如果 Maven 通过 SDKMAN 安装，仓库脚本会自动加载 SDKMAN

常用入口：

```bash
bash bin/vaadin-desktop dev examples/counter-app
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

普通 `dev`、`dev-swift` 和 `dev-ipc` 命令会打开桌面窗口，并保持运行直到 app 或窗口关闭。

## 运行模式

| 模式 | 命令 | 传输 | 用途 |
| --- | --- | --- | --- |
| `dev` | `bash bin/vaadin-desktop dev examples/counter-app` | Local HTTP compatibility | 默认开发循环，使用原生 macOS WebView host。 |
| `dev-swift` | `bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app` | Local HTTP compatibility | Swift/AppKit host 路径，包含 Java sidecar 生命周期和 host protocol trace。 |
| `dev-ipc` | `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app` | `VFIPC1` stdin/stdout IPC | no-localhost Flow IPC 切片，覆盖当前 bootstrap/event/patch/resource/capability 验证面。 |

Compatibility mode 的 host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

## 验证

运行集中式非窗口检查：

```bash
bash bin/verify-smoke
```

`bin/verify-smoke` 会运行仓库卫生检查和聚焦的 UIDL/resource smoke checks：

```bash
bash bin/repository-hygiene-check
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

运行无人值守 framework window 检查：

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

随后根据本次验证面运行对应的 `bin/flow-ipc-trace-inspect` gate。具体 gate 名称保留在 inspector 和验收文档中，不写死在 README。

完整 `.ndjson` trace 是生成产物，不按版本逐个提交。验收契约保留在 inspector 和 smoke 脚本里。需要长期保存的 trace 应进入 CI artifact、release asset 或外部归档。

## 仓库地图

- `vaadin-desktop-core`：公开的 Java desktop runtime API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace primitives。
- `vaadin-desktop-host`：macOS system WebView fallback、compatibility host 和 Java native bridge 实现。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 路径的 Swift/AppKit/WKWebView host。
- `vaadin-desktop-flow`：Vaadin Flow / Spring Boot launcher、Flow IPC runtime 和 UIDL adapter 切片。
- `vaadin-desktop-devtools`：trace inspector scaffold 和 Flow IPC gates。
- `vaadin-desktop-cli`：CLI scaffold；shell 入口是 `bin/vaadin-desktop`。
- `examples/`：小而清晰的开发者示例。
- `test-apps/`：带确定性 hook 和更广断言面的验证 app。
- `flow-transport-adapter-spike`：Vaadin Flow transport 边界分析。
- `prototypes/phase0-phase1-spike`：更早的非 HTTP mock transport 和 IPC 验证。

## Examples vs Test Apps

`examples/` 用来放可读的开发者故事：

- `examples/counter-app`
- `examples/grid-app`
- `examples/desktop-capabilities-app`
- `examples/component-gallery-app`

`test-apps/` 用来放可重复验证目标：

- `test-apps/vaadin-desktop-smoke-app`
- `test-apps/flow-over-ipc-smoke-app`
- `test-apps/flow-over-ipc-framework-app`

如果一个场景既适合做文档又适合做测试，把简单的开发者故事放在 `examples/`，把完整断言面放在 `test-apps/`。

## 当前状态

已实现：

- 真实 Vaadin Flow app modules 和示例项目。
- Java desktop API：window title、notification、clipboard、file dialog、bounded file transfer 和 menu facade。
- macOS WebView compatibility host。
- Swift/AppKit host，负责 Java sidecar 生命周期和 host protocol trace。
- Flow IPC runtime 切片，对当前 smoke apps 覆盖真实 server-side UIDL bootstrap/event/patch。
- Flow client resource loading 和 frontend artifact resolver 的当前验证面证据。
- framework-shaped validation app，覆盖代表性 Flow 组件、Binder validation、常用输入组件、多值/导航/展开/菜单组件、bounded overlay/context-menu 组件、StreamResource、Upload、DownloadHandler 和部分 desktop capability calls。
- 当前验证面的基本 no-localhost frontend/resource pipeline 闭环。
- 通过 `bin/verify-smoke` 进行集中式仓库卫生和 smoke 验证。

进行中：

- rich content、app shell 和 login 组件 coverage，目标覆盖 Avatar、MessageList、MessageInput、SideNav 和 LoginOverlay。该工作仍由显式 validation gate 约束，不能描述为已完成。

仍未完成：

- 完整 Flow client event coverage 和 UIDL parity。
- dev-mode/Vite parity，以及泛化 frontend owner/runtime reuse。
- durable push、reconnect 和 resync semantics。
- 完整 Grid virtualization 和大文件 Upload/Download streaming。
- 完整 overlay focus/keyboard/native context-menu behavior、rich overlay behavior、theme variants 和更深的 focus/accessibility 行为。
- 生产级 native capability permission model。
- packaging、signing 和 auto-update。

## 文档

- [Getting Started](docs/getting-started.md)
- [Framework Architecture](docs/framework-architecture.md)
- [Flow Integration](docs/flow-integration.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [IPC Protocol](docs/ipc-protocol.md)
- [IPC Protocol v1](docs/ipc-protocol-v1.md)
- [Acceptance and Smoke Tools](docs/acceptance-tools.md)
- [Repository Hygiene and Test Layout](docs/repository-hygiene.md)
- [Native Host](docs/native-host.md)
- [Roadmap](docs/roadmap.md)
- [Original Design Package](docs/flow-over-ipc-design.md)
