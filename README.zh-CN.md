# vatori

vatori 是一个基于 Vaadin 的 Desktop Framework，用于构建桌面级 Vaadin Flow 应用的实验性框架骨架。它的方向类似 Tauri 加 Vaadin Flow：保留 Java Flow 编程模型，运行在原生桌面 host 中，并逐步把浏览器/服务器传输假设收束到明确的 host 和 IPC 边界后面。

这不是一个已经完成的应用壳，也还不是最终版 Flow over IPC runtime。当前仓库有两条有用的运行路径：

- **Compatibility mode**：真实 Vaadin Flow / Spring Boot app 运行在 `127.0.0.1`，原生 macOS WebView host 打开桌面窗口。
- **Flow IPC 切片**：运行应用标准 Vaadin production bundle 的实验性 no-localhost 路径。浏览器请求通过 opaque IPC bridge 交给 Vaadin 正常的 request-handler chain。

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
bash bin/vaadin-desktop dev-ipc examples/counter-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

普通 `dev`、`dev-swift` 和 `dev-ipc` 命令会打开桌面窗口，并保持运行直到 app 或窗口关闭。

## 运行模式

| 模式 | 命令 | 传输 | 用途 |
| --- | --- | --- | --- |
| `dev` | `bash bin/vaadin-desktop dev examples/counter-app` | Local HTTP compatibility | 默认开发循环，使用原生 macOS WebView host。 |
| `dev-swift` | `bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app` | Local HTTP compatibility | Swift/AppKit host 路径，包含 Java sidecar 生命周期和 host protocol trace。 |
| `dev-ipc` | `bash bin/vaadin-desktop dev-ipc examples/counter-app` | 基于 `VFIPC1` 的 opaque request IPC | no-localhost 标准 Vaadin production bundle 路径，覆盖当前 Flow 验证面。 |

Compatibility mode 的 host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

`system-webview` 默认 fail-fast。需要 compatibility host 时，显式使用
`VAADIN_DESKTOP_HOST=compatibility`、`headless` 或 `browser`。如果本地开发确实
需要从 `system-webview` 自动退回 compatibility，可设置
`VAADIN_DESKTOP_HOST_FALLBACK=compatibility`。

## 验证

运行集中式非窗口检查：

```bash
bash bin/verify-smoke
```

`bin/verify-smoke` 会运行仓库卫生检查、inspector 契约检查、opaque HTTP
adapter、Flow IPC scope 和浏览器 bridge 测试。具体子命令以脚本本身为准。

真实窗口自动化和 trace gate 验收记录在 `docs/acceptance-tools.md`。具体 gate
名称保留在 inspector 和验收文档中，不写死在 README。

完整 `.ndjson` trace 是生成产物，不按版本逐个提交。验收契约保留在 inspector 和 smoke 脚本里。需要长期保存的 trace 应进入 CI artifact、release asset 或外部归档。

## 仓库地图

- `vaadin-desktop-core`：公开的 Java desktop runtime API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace primitives。
- `vaadin-desktop-host`：Java host abstraction、显式 compatibility host 和 native bridge support。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 路径的 Swift/AppKit/WKWebView host。
- `vaadin-desktop-flow`：Vaadin Flow / Spring Boot launcher、标准 production bundle runtime 和 opaque IPC transport adapter。
- `vaadin-desktop-devtools`：标准 production bundle 的 trace inspector 和 Flow IPC gates。
- `vaadin-desktop-cli`：CLI scaffold；shell 入口是 `bin/vaadin-desktop`。
- `bin/`：本地开发、smoke 检查和仓库卫生检查的 shell 入口。
- `docs/`：当前架构、runtime、protocol 和 acceptance 文档。
- `assets/`：品牌源文件和正式导出资产。
- `.agents/`：仓库本地 Codex workflow skills。
- `examples/`：小而清晰的开发者示例。
- `test-apps/`：带确定性 hook 和更广断言面的验证 app。

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

## 状态和路线

稳定路线在 `docs/roadmap.md`。当前 Flow IPC runtime 边界在
`docs/flow-over-ipc-runtime.md`；验收工具在 `docs/acceptance-tools.md`。

不要把当前 Flow IPC validation surface 描述为完整产品级 parity。描述当前行为时，
必须明确 compatibility mode、focused validation gate、fallback classification 和
blocker diagnostics 的边界。

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
