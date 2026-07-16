# 仓库结构与心智模型

本文只描述长期所有权、依赖方向和代码放置规则，不记录当前 goal、单次验收结果或
实现过程。

## 运行主链

```text
bin/vaadin-desktop
  -> native-hosts/macos
  -> AppKit + WKWebView
  -> Java sidecar
  -> vaadin-desktop-flow
```

平台和构建产物决定 native host。Java 应用不选择 host，也不包含 host fallback。
当前只有 macOS 实现；未来平台实现放在 `native-hosts/<platform>`，并遵守同一个
sidecar contract。

两种 UI transport 复用同一条 native capability 与生命周期通道：

```text
flow-ipc
  standard Vaadin production bundle
    -> opaque browser bridge -> VFIPC1 -> Vaadin request-handler chain

compatibility-http
  loopback Vaadin URL
    -> WKWebView

both
  Java Desktop API <-> VFIPC1 command/capabilityResult <-> native host
```

## 发布模块

| 目录 | 所有权 | 不应包含 |
| --- | --- | --- |
| `vaadin-desktop-core` | 开发者使用的 desktop API、application/window model、capability contract | Vaadin transport、平台实现、测试 fixture |
| `vaadin-desktop-ipc` | `flow-ipc-v1` envelope 与通用 JSON/trace primitive | Flow 组件语义、host UI |
| `vaadin-desktop-flow` | `DesktopLauncher`、Spring/Vaadin sidecar runtime、opaque request/push adapter、统一 native bridge | 平台 host 选择、自定义 Flow renderer、应用 fixture |
| `vaadin-desktop-cli` | 可发布 CLI artifact 的入口 | 仓库 smoke orchestration、平台实现 |
| `native-hosts/macos` | AppKit/WKWebView host、Java sidecar 生命周期、标准 bundle 装载、macOS capability 执行 | acceptance flags、DOM 断言、故障注入 |

Java 依赖方向固定为：

```text
core <- ipc <- flow
```

`flow` 可以同时依赖 `core` 和 `ipc`。框架模块不得依赖 `examples`、`test-apps`
或 `test-support`。standard production bundle 由 CLI/native-host build 生成和打包，
不属于 `vaadin-desktop-flow` 的源码资源。

## 应用目录

| 目录 | 用途 |
| --- | --- |
| `examples` | 面向开发者阅读的最小接入和能力示例 |
| `test-apps` | 带稳定 DOM id、确定性状态和广覆盖断言面的验证应用 |

两者都应像外部项目一样使用 `@SpringBootApplication`、`DesktopLauncher` 和公开
framework artifact。框架源码不得引用应用类或应用资源。

## 测试支持

| 目录 | 用途 |
| --- | --- |
| `test-support/native-host-macos-acceptance` | 显式 acceptance build 使用的 WKWebView 自动化、截图、多窗口和 sidecar recovery 驱动 |
| `test-support/vaadin-desktop-trace-inspector` | 读取 `HOST_PROTOCOL` trace 的独立验收工具 |

默认 macOS host build 只编译 production sources。只有显式 acceptance build 才加入
test-support 源码。`test-support` 不进入根 Maven reactor，也不能成为 framework
artifact 的依赖。生产 CLI、builder 和 launcher 不能识别 acceptance 环境变量或
`test-support` 路径；验收 host 由 `test-support/native-host-macos-acceptance/bin`
独立组装和启动。

## 仓库配套目录

| 目录 | 用途 |
| --- | --- |
| `bin` | 开发入口、platform host build、smoke、trace 与 hygiene 命令 |
| `docs` | 当前架构、协议、runtime 边界、验收和路线图 |
| `assets` | 品牌源文件和正式导出资产 |
| `.agents` | 仓库本地 skills 与可复用 agent workflow |

`.build`、`.m2` 和各模块 `target` 都是本地生成物，不表达架构所有权，也不加入
版本管理。

## 放置规则

新增代码时依次判断：

1. 外部应用是否直接调用？放 `vaadin-desktop-core`。
2. 是否只定义跨进程 envelope/序列化？放 `vaadin-desktop-ipc`。
3. 是否依赖 Vaadin request、session、route、push 或 Spring sidecar？放 `vaadin-desktop-flow`。
4. 是否直接操作某个平台 WebView、窗口、sidecar 进程或 capability？放对应 `native-hosts/<platform>`。
5. 是否只服务自动化、故障注入、截图或 gate？放 `test-support`。
6. 是否是可读用户故事？放 `examples`；以机器断言为主则放 `test-apps`。

如果一段代码同时属于平台 runtime 和测试应用，先提取稳定 extension point，再把
production implementation 与 acceptance overlay 分开放置。
